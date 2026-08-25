package com.mealos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.mealos.ui.MealOsApp
import com.mealos.ui.theme.MealOsTheme
import com.mealos.timer.TimerNotifications
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : ComponentActivity() {
    private val viewModel: MealOsViewModel by viewModels()
    private val credentialManager by lazy { CredentialManager.create(this) }
    private var timerAlertsAvailable by mutableStateOf(false)
    private var preciseTimersAvailable by mutableStateOf(false)
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTimerAccess()
    }
    private val recipeScanner = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) {
            viewModel.setAssistantScanning(false)
            return@registerForActivityResult
        }
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        val pages = result?.pages?.map { it.imageUri }.orEmpty()
        if (pages.isEmpty()) viewModel.assistantScanFailed("The scan did not contain any pages.")
        else recognizeRecipePages(pages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleRecipeIntent(intent)
        enableEdgeToEdge()
        setContent {
            val fold by produceState<FoldingFeature?>(initialValue = null) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info -> value = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
            }
            MealOsTheme {
                MealOsApp(
                    viewModel = viewModel,
                    fold = fold,
                    timerAlertsAvailable = timerAlertsAvailable,
                    preciseTimersAvailable = preciseTimersAvailable,
                    onEnableTimerAlerts = ::enableTimerAlerts,
                    onEnablePreciseTimers = ::enablePreciseTimers,
                    onScanRecipe = ::scanRecipe,
                    onSignIn = ::signInWithGoogle,
                    onSignOut = ::signOut
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRecipeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshTimerAccess()
        viewModel.rescheduleRunningTimers()
    }

    private fun refreshTimerAccess() {
        timerAlertsAvailable = TimerNotifications.canPostAlerts(this)
        preciseTimersAvailable = TimerNotifications.canScheduleExactAlarms(this)
    }

    private fun enableTimerAlerts() {
        val runtimePermissionMissing = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val permissionHistory = getSharedPreferences("mealos_permissions", MODE_PRIVATE)
        val askedBefore = permissionHistory.getBoolean("notification_asked", false)
        if (runtimePermissionMissing && (!askedBefore || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            permissionHistory.edit { putBoolean("notification_asked", true) }
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(TimerNotifications.notificationSettingsIntent(this))
        }
    }

    private fun enablePreciseTimers() {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    }

    private fun scanRecipe() {
        viewModel.setAssistantScanning(true)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(8)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(this)
            .addOnSuccessListener { sender -> recipeScanner.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener { error ->
                viewModel.assistantScanFailed(error.message ?: "The document scanner is unavailable on this device.")
            }
    }

    private fun signInWithGoogle() {
        viewModel.beginGoogleSignIn()
        lifecycleScope.launch {
            runCatching {
                val googleOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build()
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
                )
                val credential = result.credential as? CustomCredential
                    ?: error("Google did not return a usable account credential.")
                check(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    "Google returned an unsupported account credential."
                }
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null))
                    .awaitFirebase()
            }.onSuccess {
                viewModel.finishGoogleSignIn()
            }.onFailure { error ->
                viewModel.finishGoogleSignIn(
                    if (error is GetCredentialCancellationException) null
                    else error.message ?: "Google sign-in could not be completed. Try again."
                )
            }
        }
    }

    private fun signOut() {
        viewModel.signOut()
        lifecycleScope.launch {
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        }
    }

    private fun recognizeRecipePages(pages: List<Uri>) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = mutableListOf<String>()
        fun recognize(index: Int) {
            if (index >= pages.size) {
                recognizer.close()
                val combined = text.mapIndexed { page, value -> "--- Page ${page + 1} ---\n$value" }.joinToString("\n\n")
                viewModel.attachScannedRecipe(combined, pages.size)
                return
            }
            runCatching { InputImage.fromFilePath(this, pages[index]) }
                .onFailure {
                    recognizer.close()
                    viewModel.assistantScanFailed("MealOS could not open page ${index + 1} of the scan.")
                }
                .onSuccess { image ->
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            text += result.text
                            recognize(index + 1)
                        }
                        .addOnFailureListener { error ->
                            recognizer.close()
                            viewModel.assistantScanFailed(error.message ?: "Text extraction failed on page ${index + 1}.")
                        }
                }
        }
        recognize(0)
    }

    private fun handleRecipeIntent(intent: Intent?) {
        if (intent == null || intent.action == Intent.ACTION_MAIN) return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val sharedFile = intent.sharedStreamUri()
        val content = sharedText ?: (intent.data ?: sharedFile ?: intent.clipData?.getItemAt(0)?.uri)?.let { uri ->
            runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        }
        if (!content.isNullOrBlank()) {
            viewModel.importRecipe(content)
            intent.action = Intent.ACTION_MAIN
            intent.data = null
            intent.clipData = null
            intent.replaceExtras(Bundle())
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

private suspend fun <T> Task<T>.awaitFirebase(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

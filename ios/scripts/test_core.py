#!/usr/bin/env python3
"""Run the XCTest test bodies with Command Line Tools when XCTest is unavailable.
Xcode/CI uses swift test. This small adapter only implements assertions used here.
"""
import json, os, pathlib, re, subprocess, tempfile
root=pathlib.Path(__file__).resolve().parents[1]
test=root/'Tests/MealOSCoreTests/MealOSCoreTests.swift'
source=test.read_text().replace('import XCTest','import Foundation').replace('@testable import MealOSCore','').replace('#filePath',json.dumps(str(test)))
names=re.findall(r'    func (test\w+)\(\) throws',source)
shim='''
class XCTestCase {}
func XCTAssertEqual<T: Equatable>(_ lhs: @autoclosure () throws -> T, _ rhs: @autoclosure () throws -> T, _ message: String = "", file: StaticString = #filePath, line: UInt = #line) {
    do { let a = try lhs(); let b = try rhs(); if a != b { fatalError("\\(file):\\(line): \\(a) != \\(b) \\(message)") } } catch { fatalError("Unexpected error: \\(error)") }
}
func XCTAssertNotEqual<T: Equatable>(_ lhs: T, _ rhs: T) { if lhs == rhs { fatalError("Expected unequal values") } }
func XCTAssertTrue(_ value: @autoclosure () throws -> Bool, _ message: String = "") { do { if try !value() { fatalError("Expected true: \\(message)") } } catch { fatalError("Unexpected error: \\(error)") } }
func XCTAssertFalse(_ value: @autoclosure () -> Bool, _ message: String = "") { if value() { fatalError("Expected false: \\(message)") } }
func XCTAssertNil<T>(_ value: T?) { if value != nil { fatalError("Expected nil") } }
func XCTAssertThrowsError<T>(_ value: @autoclosure () throws -> T) { do { _ = try value() } catch { return }; fatalError("Expected an error") }
func XCTUnwrap<T>(_ value: T?) throws -> T { guard let value else { throw MealOSError("Expected a value") }; return value }
'''
with tempfile.TemporaryDirectory(prefix='mealos-core-') as folder:
    folder=pathlib.Path(folder)
    main=folder/'main.swift'
    main.write_text(shim+source+'\nlet suite = MealOSCoreTests()\n'+''.join(f'try suite.{name}(); print("PASS {name}")\n' for name in names)+f'print("{len(names)} core tests passed")\n')
    subprocess.run(['swiftc','-module-cache-path','/tmp/mealos-core-module-cache',*map(str,sorted((root/'MealOS/Core').glob('*.swift'))),str(main),'-o',str(folder/'tests')],check=True)
    subprocess.run([str(folder/'tests')],check=True)

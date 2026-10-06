#!/usr/bin/env python3
"""Parse the catalog's small literal Kotlin DSL; never evaluate source code."""
import json, pathlib, re
root = pathlib.Path(__file__).resolve().parents[2]
source = (root / 'app/src/main/java/com/mealos/data/RecipeCatalog.kt').read_text()
source = source[source.index('listOf('):source.index('\n    fun recipe')].strip()
tokens = re.findall(r'"(?:\\.|[^"\\])*"|[A-Za-z_][A-Za-z_0-9]*|\d+(?:\.\d+)?|[(),=*]', source)
pos = 0

def value():
    global pos
    t = tokens[pos]; pos += 1
    if t.startswith('"'): return json.loads(t)
    if t[0].isdigit():
        n = float(t) if '.' in t else int(t)
        if pos < len(tokens) and tokens[pos] == '*':
            pos += 1; n *= value()
        return n
    if t in ('true', 'false'): return t == 'true'
    if pos >= len(tokens) or tokens[pos] != '(': return t
    pos += 1; args = []; kwargs = {}
    while tokens[pos] != ')':
        if tokens[pos + 1] == '=':
            key = tokens[pos]; pos += 2; kwargs[key] = value()
        else: args.append(value())
        if tokens[pos] == ',': pos += 1
    pos += 1
    if t == 'listOf': return args
    fields = {'recipe': ['id','name','shortName','effort','total','equipment','tags','leftovers','ingredients','steps'],
              'ing': ['id','name','amount','unit','category','pantry'],
              'step': ['title','instruction','equipment','timer','tip']}[t]
    result = dict(zip(fields, args)); result.update(kwargs)
    if t == 'recipe':
        for old, new in [('effort','effortMinutes'),('total','totalMinutes'),('leftovers','leftoverNote')]: result[new] = result.pop(old)
        result.update(baseServings=2, description='', origin='CORE')
    if t == 'ing': result['pantryStaple'] = result.pop('pantry', False)
    if t == 'step' and 'timer' in result: result['timerSeconds'] = result.pop('timer')
    return result

recipes = value()
assert len(recipes) == 15 and len({r['id'] for r in recipes}) == 15
out = root / 'ios/MealOS/Resources/recipes.json'
out.write_text(json.dumps(recipes, ensure_ascii=False, indent=2) + '\n')
print(f'Exported {len(recipes)} original recipes to {out}')

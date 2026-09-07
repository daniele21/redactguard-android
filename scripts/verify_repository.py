#!/usr/bin/env python3
import json,sys
from pathlib import Path
r=Path('.'); e=[]
for p in ['AGENTS.md','.engineering/baseline.json','.engineering/product.json','.engineering/commands.json','.engineering/e2e.json','.engineering/documentation-policy.json','docs/product.md','skills/shape-product-change/SKILL.md','scripts/verify_operations.py','scripts/verify_e2e.py','scripts/verify_stage_environment_policy.py','scripts/verify_product_development.py','scripts/verify_agent_context.py']:
    if not (r/p).is_file(): e.append(f'missing {p}')
try:b=json.loads((r/'.engineering/baseline.json').read_text())
except Exception as x:e.append(str(x));b={}
if b.get('standard',{}).get('source')!='daniele21/repo-template-sw':e.append('baseline source mismatch')
if b.get('standard',{}).get('version')!='0.11.0':e.append('baseline version must be 0.11.0')
shape=b.get('skills',{}).get('shape-product-change')
if not isinstance(shape,dict) or shape.get('source_version')!='0.11.0' or not isinstance(shape.get('customized'),bool):e.append('invalid shape-product-change skill metadata')
print('Repository baseline check');[print('FAIL:',x) for x in e];print('RESULT:','FAIL' if e else 'PASS');sys.exit(bool(e))

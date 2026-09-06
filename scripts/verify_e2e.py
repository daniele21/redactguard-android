#!/usr/bin/env python3
import json,sys
from pathlib import Path
d=json.loads(Path('.engineering/e2e.json').read_text());e=[]
if d.get('contract_version')!='0.2.1':e.append('contract_version must be 0.2.1')
i=d.get('stage_policy',{}).get('integration',{});r=d.get('stage_policy',{}).get('release',{})
if i.get('automated_e2e_before_shared_integration') is not True:e.append('integration automated E2E required')
if i.get('real_environment_blocking') is not False or i.get('real_environment_deferred_to_release') is not True:e.append('integration real environment must defer')
if i.get('material_ui_journey_minimum_evidence_mode')!='full_media':e.append('material UI integration must use full_media')
for k in ['full_validation_required','release_critical_e2e_required','required_real_environment_blocking']:
    if r.get(k) is not True:e.append('release.'+k+' must be true')
print('E2E environment contract check');[print('FAIL:',x) for x in e];print('RESULT:','FAIL' if e else 'PASS');sys.exit(bool(e))

#!/usr/bin/env python3
import json,sys
from pathlib import Path
c=json.loads(Path('.engineering/commands.json').read_text());d=json.loads(Path('.engineering/e2e.json').read_text());e=[]
ci=c['development_velocity']['integration'];cr=c['development_velocity']['release'];ei=d['stage_policy']['integration'];er=d['stage_policy']['release']
checks=[ci.get('automated_e2e_required_when_affected') is True,ci.get('real_environment_blocking') is False,ci.get('real_environment_deferred_to_release') is True,cr.get('required_real_environment_blocking') is True,ei.get('automated_e2e_before_shared_integration') is True,ei.get('real_environment_blocking') is False,ei.get('real_environment_deferred_to_release') is True,er.get('required_real_environment_blocking') is True]
if not all(checks):e.append('commands/e2e stage policy mismatch')
print('Stage/environment policy check');[print('FAIL:',x) for x in e];print('RESULT:','FAIL' if e else 'PASS');sys.exit(bool(e))

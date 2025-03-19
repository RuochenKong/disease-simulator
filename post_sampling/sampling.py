import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
import sys
import random

disease_version = sys.argv[-1]
random.seed(10)

all_ver_dir = '../examples/logs/'
log_dir = '%s%s/' % (all_ver_dir, disease_version)
disease_report_fn = 'DiseaseStatusChangeJournal.tsv'
agent_char_fn = 'AgentCharacteristicsTable.tsv'
single_params_fn = 'single_params'
multi_params_fn = 'multi_params'

age_bias = None
gender_bias = None
income_bias = None
race_bias = None
edu_bias = None
single_version = 1
do_single = True
with open(single_params_fn, 'r') as f:
    for line in f.readlines():
        if line.strip() == 'skip':
            do_single = False
            break

        flag,text = line.strip().split(' = ')
        if flag == 'ageProb':
            age_bias = np.zeros(100)
            tmp = text.split('/')
            for pair in tmp:
                val_range,chance = pair.split(':')
                if val_range == 'other':
                    age_bias = np.where(age_bias == 0, float(chance), age_bias)
                else:
                    val_range = val_range[1:-1]
                    low,high = val_range.split('-')
                    age_bias[int(low)-1:int(high)-1] = float(chance)
        elif flag == 'incomeProb':
            income_bias = np.zeros(500)
            tmp = text.split('/')
            for pair in tmp:
                val_range,chance = pair.split(':')
                if val_range == 'other':
                        income_bias = np.where(income_bias == 0, float(chance), income_bias)
                else:
                    val_range = val_range[1:-1]
                    low,high = val_range.split('-')
                    high = 500 if high == 'Inf' else int(high)-1
                    income_bias[int(low)-1:high] = float(chance)
        elif flag == 'genderProb':
            gender_bias = {}
            tmp = text.split('/')
            for pair in tmp:
                gender,chance = pair.split(':')
                gender_bias[gender] = float(chance)
        elif flag == 'raceProb':
            race_bias = {}
            tmp = text.split('/')
            for pair in tmp:
                race,chance = pair.split(':')
                race_bias[race] = float(chance)
        elif flag == 'eduProb':
            edu_bias = {}
            tmp = text.split('/')
            for pair in tmp:
                edu,chance = pair.split(':')
                edu_bias[edu] = float(chance)
        elif flag == 'version':
            single_version = int(text)


multivariate = {}
multi_version = 1
do_multi = True
with open(multi_params_fn, 'r') as f:
    for line in f.readlines():
        if line.strip() == 'skip':
            do_multi = False
            break

        flag,text = line.strip().split(' = ')
        if flag == 'biasConsideration':
            tmp = text.split('/')
            for bias in tmp:
                multivariate[bias.lower()] = 0
        elif flag == 'intercept':
            multivariate['intercept'] = float(text)
        elif flag == 'version':
            multi_version = int(text)
        else:
            flag = flag.replace('White', '')
            flag = flag.replace('Male', '')
            if flag in multivariate.keys():
                multivariate[flag] = float(text)


agent_char = pd.read_csv(log_dir + agent_char_fn, sep='\t', engine="python")
agent_char = agent_char.rename(columns={'originRegionId':'rid', 'indiv_census_income':'income'})
selected_columns = ['agentId', 'rid', 'isMale', 'age', 'race', 'income', 'educationLevel']
agent_char = agent_char[selected_columns].reset_index(drop = True)
agent_char['isWhite'] = agent_char['race'] == 'WhiteOnly'
agent_char['over70k'] = agent_char['income'] >= 70000
agent_char['isVulnerable'] = agent_char['age'] > 50
agent_char['odd vulnerability'] = 1

if do_multi:
    for key,val in multivariate.items():
        if key == 'intercept': continue
        if key == 'vulnerability':
            agent_char['odd vulnerability'] = 1.0
            agent_char.loc[agent_char[agent_char['isVulnerable']].index,'odd vulnerability'] = val
        if key == 'gender':
            agent_char['odd gender'] = 1.0
            agent_char.loc[agent_char[agent_char['isMale']].index,'odd gender'] = val
        if key == 'income':
            agent_char['odd income'] = 1.0
            agent_char.loc[agent_char[agent_char['over70k']].index,'odd income'] = val
        if key == 'race':
            agent_char['odd race'] = 1.0
            agent_char.loc[agent_char[agent_char['isWhite']].index,'odd race'] = val

    agent_char['multi_rate'] = multivariate['intercept']
    for key,_ in multivariate.items():
        if key == 'intercept': continue
        agent_char['multi_rate'] = agent_char['multi_rate'] * agent_char['odd %s'%key]
    agent_char['multi_rate'] = agent_char['multi_rate'] / (1+ agent_char['multi_rate'])
    agent_char = agent_char[selected_columns+['multi_rate']].reset_index(drop = True)

    agent_char['multi_report'] = False
    for i in range(5000):
        agent_info = agent_char.iloc[i]
        if random.random() < agent_info['multi_rate']: agent_char.loc[i,'multi_report'] = True
    selected_columns += ['multi_report']


if do_single:
    agent_char['age_report'] = False
    agent_char['income_report'] = False
    agent_char['race_report'] = False
    agent_char['gender_report'] = False
    agent_char['edu_report'] = False

    for i in range(len(agent_char)):
        agent_info = agent_char.iloc[i]
        if age_bias is not None:
            if random.random() < age_bias[int(agent_info['age'])]: agent_char.loc[i, 'age_report'] = True
        if income_bias is not None:
            if random.random() < income_bias[min(499, int(agent_info['income']/1000))]: agent_char.loc[i,'income_report'] = True
        if race_bias is not None:
            race_rate = race_bias['other'] if agent_info['race'] not in race_bias else race_bias[agent_info['race']]
            if random.random() < race_rate: agent_char.loc[i,'race_report'] = True
        if gender_bias is not None:
            gender_rate = gender_bias['Male'] if agent_info['isMale'] else gender_bias['Female']
            if random.random() < gender_rate: agent_char.loc[i,'gender_report'] = True
        if edu_bias is not None:
            edu_rate = edu_bias['other'] if agent_info['educationLevel'] not in edu_bias else edu_bias[agent_info['educationLevel']]
            if random.random() < edu_rate: agent_char.loc[i,'edu_report'] = True

    agent_char.to_csv('check.csv')
    if age_bias is not None: selected_columns += ['age_report']
    if income_bias is not None: selected_columns += ['income_report']
    if race_bias is not None: selected_columns += ['race_report']
    if gender_bias is not None: selected_columns += ['gender_report']
    if edu_bias is not None: selected_columns += ['edu_report']

agent_char = agent_char[selected_columns].reset_index(drop = True)

disease_report = pd.read_csv(log_dir + disease_report_fn, sep ='\t|,', engine="python")
initial_cases = disease_report.iloc[:5000]
initial_cases = initial_cases[initial_cases['diseaseStatus'] == 'Infectious'].reset_index(drop = True)
disease_report = disease_report.iloc[5000:].reset_index(drop = True)
disease_report = pd.concat([initial_cases,disease_report], ignore_index=True)

if not os.path.exists(log_dir + 'multi-biases-sampled'):
    os.mkdir(log_dir + 'multi-biases-sampled')
if not os.path.exists(log_dir + 'single-bias-sampled'):
    os.mkdir(log_dir + 'single-bias-sampled')

if do_multi:
    reported_agents = agent_char[agent_char['multi_report']]['agentId'].tolist()
    reported_cases = disease_report[disease_report['agentId'].isin(reported_agents)].reset_index(drop = True)
    reported_cases.to_csv('test.csv')
    for i in range(len(reported_cases)):
        disease_seq = reported_cases.loc[i, 'diseaseSeq']
        if disease_seq is np.nan:
            reported_cases.loc[i, 'diseaseSeq'] = ''
            continue
        disease_seq = disease_seq.split('.')
        last_unreported_idx = -1
        for k,case in enumerate(disease_seq):
            case = int(case.split('-')[0])
            if case not in reported_agents:
                last_unreported_idx = k
        if last_unreported_idx != -1:
            update_seq = '.'.join(disease_seq[last_unreported_idx+1:])
            update_seq = '?.'+update_seq
            reported_cases.loc[i, 'diseaseSeq'] = update_seq
    reported_cases.to_csv(log_dir + 'multi-biases-sampled/v%d.csv' % (multi_version), index = False)

    with open(log_dir + 'multi-biases-sampled/v%d_params' % multi_version, 'w+') as fout:
        fout.write(open(multi_params_fn).read())

if do_single:
    reports = [report_type for report_type in selected_columns if 'report' in report_type and 'multi' not in report_type]
    for report_type in reports:
        reported_agents = agent_char[agent_char[report_type]]['agentId'].tolist()
        reported_cases = disease_report[disease_report['agentId'].isin(reported_agents)].reset_index(drop = True)
        for i in range(len(reported_cases)):
            disease_seq = reported_cases.loc[i, 'diseaseSeq']
            if disease_seq is np.nan:
                reported_cases.loc[i, 'diseaseSeq'] = ''
                continue
            disease_seq = disease_seq.split('.')
            update_seq = ''
            for case in disease_seq:
                if len(update_seq) != 0: update_seq += '.'
                update_seq += case
                case = int(case.split('-')[0])
                if case not in reported_agents:
                    update_seq = '?'
            reported_cases.loc[i, 'diseaseSeq'] = update_seq
        reported_cases.to_csv(log_dir + 'single-bias-sampled/%s_v%d.csv' % (report_type.replace('_report', ''), single_version), index = False)

    with open(log_dir + 'single-bias-sampled/v%d_params' % single_version, 'w+') as fout:
        fout.write(open(single_params_fn).read())

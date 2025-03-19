import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import sys

disease_version = sys.argv[-1]
log_dir = '../examples/logs/'

disease_df = pd.read_csv(log_dir + disease_version + '/DiseaseStatusChangeJournal.tsv', sep='\t|,', engine='python')
initial_cases = disease_df.iloc[:5000]
initial_cases = initial_cases[initial_cases['diseaseStatus'] == 'Infectious'].reset_index(drop = True)
disease_df = disease_df.iloc[5000:].reset_index(drop = True)
disease_df = pd.concat([initial_cases,disease_df], ignore_index=True)

count_S = np.ones(25920) * 5000
count_I = np.zeros(25920)
count_E = np.zeros(25920)
count_R = np.zeros(25920)

for i in range(len(disease_df)):
    case_info = disease_df.iloc[i]
    step = int(case_info['step'])
    if case_info['diseaseStatus'] == 'Exposed':
        count_E[step:] += 1
        count_S[step:] -= 1
    elif case_info['diseaseStatus'] == 'Infectious':
        count_E[step:] -= 1
        count_I[step:] += 1
    elif case_info['diseaseStatus'] == 'Recovered':
        count_I[step:] -= 1
        count_R[step:] += 1
    else:
        count_R[step:] -= 1
        count_S[step:] += 1

plt.figure(figsize=(5,3), dpi = 100)
x = np.arange(25920)
x_scaled = x / 288
plt.plot(x_scaled, count_S, label = 'Susceptible')
plt.plot(x_scaled, count_E, label = 'Exposed')
plt.plot(x_scaled, count_I, label = 'Infectious')
plt.plot(x_scaled, count_R, label = 'Recovered')
plt.xlabel('Day')
plt.ylabel('Number of Agents')
plt.legend(loc = 'upper right')
plt.savefig(log_dir + disease_version +'/figures/epi-curve.pdf', format = 'pdf',  bbox_inches='tight')
plt.close()

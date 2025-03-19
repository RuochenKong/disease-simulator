import geopandas as gpd
import pandas as pd
import os
import matplotlib.pyplot as plt
import  sys

disease_version = sys.argv[-1]
log_dir = '../examples/logs/'
map_fn = 'atlanta' if 'atl' in disease_version else 'san-fran'
map_fn = '../examples/'+map_fn+'/region_census.shp'

if not os.path.exists(log_dir + disease_version + '/figures'):
    os.mkdir(log_dir + disease_version + '/figures')

single_bias = ['age', 'gender', 'income', 'race']
single_dir = 'single-bias-sampled/'
single_fn = single_dir + '%s_v%d.csv'
multi_fn = 'multi-biases-sampled/v%d.csv'

# Age group: 15(0), 20(1), 25(2), 30(3), 35(4), 40(5), 45(6), 50(7), 55(8), 60(9)
# IndiInc (Individual Income): 0(0), 10(1), 25(2), 50(3), 75(4), 100(5)
# Race: WhiteOnly(0), BlackOnly(1), AmerIndianOnly(2), AsianOnly(3), PacIslandOnly(4), Other(5), Plus2Races(6)
# Education: Unknown (0), Low (1), HighSchoolOrCollege (2), Bachelors (3), Graduate (4)
map_geo_df = gpd.read_file(map_fn) 
map_geo_df['Female'] = 1-map_geo_df['Male']
map_geo_df['Non-White'] = 1-map_geo_df['Race0']
map_geo_df['Income >= 50k'] = map_geo_df['IndiInc3']+map_geo_df['IndiInc4']+map_geo_df['IndiInc5']
map_geo_df['Age >= 50'] = map_geo_df['AgeGroup7']+map_geo_df['AgeGroup8']+map_geo_df['AgeGroup9']
map_geo_df['Edu >= Bachelors'] = map_geo_df['EduLevel3'] + map_geo_df['EduLevel4']
map_geo_df = map_geo_df[['TrackId', 'id', 'geometry', 'TotPop', 'Age >= 50', 'Female', 'Non-White', 'Income >= 50k', 'Edu >= Bachelors']].reset_index(drop=True)

agent_char_df = pd.read_csv(log_dir+disease_version+'/AgentCharacteristicsTable.tsv', sep='\t', engine="python")
simu_pop_count = agent_char_df.groupby('originRegionId').count()['agentId'].reset_index()
simu_pop_count = simu_pop_count.rename(columns={'originRegionId':'id', 'agentId':'simu-pop'})
map_geo_df=map_geo_df.merge(simu_pop_count, on='id', how='left')

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='simu-pop', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/simu_pop.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='Age >= 50', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/vulnerable.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='Female', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/female.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='Income >= 50k', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/rich.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='Non-White', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/non-white.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='Edu >= Bachelors', cmap='YlGn', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/high-edu.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

disease_truth_df = pd.read_csv(log_dir+disease_version+'/DiseaseStatusChangeJournal.tsv', sep='\t|,', engine="python")
disease_truth_df = disease_truth_df[disease_truth_df['diseaseStatus'] == 'Infectious'].reset_index(drop=True)
disease_truth_df['id'] = disease_truth_df['[regionId'].apply(lambda x : int(x[1:]))
disease_truth_count_df = disease_truth_df.groupby('id').count()['agentId'].reset_index()
disease_truth_count_df = disease_truth_count_df.rename(columns={'agentId':'total infectious'})

map_geo_df = map_geo_df.merge(disease_truth_count_df, on='id')
fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='total infectious', cmap='PuBu', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/true_case_infectious.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

map_geo_df['total infectious'] = map_geo_df['total infectious'] / map_geo_df['simu-pop']
fig, ax = plt.subplots(1, figsize=(5,3))
map_geo_df.plot(column='total infectious', cmap='RdPu', ax=ax, linewidth=1, vmin = 0, vmax = 1, edgecolor='0.9', legend=True)
ax.axis('off')
fig.savefig(log_dir + disease_version + '/figures/true_percent_infectious.pdf', bbox_inches='tight', format='pdf')
plt.close(fig)

if not os.path.exists(log_dir + disease_version + '/figures/multi-biases'):
    os.mkdir(log_dir + disease_version + '/figures/multi-biases')

for i in range(1,4):
    biased_disease_df = pd.read_csv(log_dir + disease_version+'/multi-biases-sampled/v%d.csv'%i, sep='\t|,', engine="python")
    biased_disease_df = biased_disease_df[biased_disease_df['diseaseStatus'] == 'Infectious'].reset_index(drop=True)
    biased_disease_df['id'] = biased_disease_df['[regionId'].apply(lambda x : int(x[1:]))
    biased_disease_df = biased_disease_df.groupby('id').count()['agentId'].reset_index()
    biased_disease_df = biased_disease_df.rename(columns={'agentId':'reported infectious'})
    map_geo_bias_df = map_geo_df.merge(biased_disease_df, on='id')
    map_geo_bias_df['reported infectious'] = map_geo_bias_df['reported infectious'] / map_geo_bias_df['simu-pop']

    fig, ax = plt.subplots(1, figsize=(5,3))
    map_geo_bias_df.plot(column='reported infectious', cmap='RdPu', ax=ax, linewidth=1, vmin = 0, vmax = 1, edgecolor='0.9', legend=True)
    ax.axis('off')
    fig.savefig(log_dir + disease_version + '/figures/multi-biases/v%d_reported.pdf'%i, bbox_inches='tight', format='pdf')
    plt.close(fig)


if not os.path.exists(log_dir + disease_version + '/figures/single-bias'):
    os.mkdir(log_dir + disease_version + '/figures/single-bias')

bias_types = ['age','gender','income','race','edu']
for bt in bias_types:
    if not os.path.exists(log_dir + disease_version + '/figures/single-bias/%s'%bt):
        os.mkdir(log_dir + disease_version + '/figures/single-bias/%s'%bt)

    for i in range(1,5):
        biased_disease_df = pd.read_csv(log_dir + disease_version+'/single-bias-sampled/%s_v%d.csv'%(bt,i), sep='\t|,', engine="python")
        biased_disease_df = biased_disease_df[biased_disease_df['diseaseStatus'] == 'Infectious'].reset_index(drop=True)
        biased_disease_df['id'] = biased_disease_df['[regionId'].apply(lambda x : int(x[1:]))
        biased_disease_df = biased_disease_df.groupby('id').count()['agentId'].reset_index()
        biased_disease_df = biased_disease_df.rename(columns={'agentId':'reported infectious'})
        map_geo_bias_df = map_geo_df.merge(biased_disease_df, on='id')
        map_geo_bias_df['reported infectious'] = map_geo_bias_df['reported infectious'] / map_geo_bias_df['simu-pop']
    
        fig, ax = plt.subplots(1, figsize=(5,3))
        map_geo_bias_df.plot(column='reported infectious', cmap='RdPu', ax=ax, linewidth=1, edgecolor='0.9', legend=True)
        ax.axis('off')
        fig.savefig(log_dir + disease_version + '/figures/single-bias/%s/v%d_reported.pdf'%(bt,i), bbox_inches='tight', format='pdf')
        plt.close(fig)
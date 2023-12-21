import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt

DISEASEDATA = '../unbiased_2000.tsv'
RegionColor = {'A':'#33658a', 'B':'#86bbd8', 'C':'#f26419', 'D':'#f6ae2d'}
RegionName = ['A', 'B', 'C', 'D']
MaxStep = 60 * 288
NumAgent = 2000 - 58

# Spliting the map into four regions
def split_origin_into_regions(display:bool = False):
    unbiased = pd.read_csv(DISEASEDATA, sep='\t', header=0)
    locx = []
    locy = []

    for loc in unbiased['OriginalLoc']:
        loc = loc.replace('POINT', '').strip().split(' ')
        locx.append(float(loc[0][1:]))
        locy.append(float(loc[1][:-1]))

    sortedlocx = sorted(locx)
    sortedlocy = sorted(locy)

    medx = sortedlocx[1070]
    medy = sortedlocy[980]

    region = []
    count = [0,0,0,0]
    for x, y in zip(locx, locy):
        if x < medx and y < medy:
            region.append('A')
            count[0] += 1
        elif x >= medx and y < medy:
            region.append('B')
            count[1] += 1
        elif x >= medx and y >= medy:
            region.append('C')
            count[2] += 1
        else:
            region.append('D')
            count[3] += 1

    unbiased['Region'] = region
    unbiased.to_csv('../processed_data/unbiased_with_region.tsv', sep="\t", index=False)
    unbiased['OriginX'] = locx
    unbiased['OriginY'] = locy

    plt.figure(1)
    unbiased.plot.scatter(x='OriginX', y='OriginY', c=unbiased['Region'].map(RegionColor),  alpha=0.8, s=3)
    plt.savefig("../figures/4_regions.png")
    plt.close()


# Create biased dataset based on different reporting rate
def report_with_rates(unbiased_fn, rate1, rate2):
    unreport_agent = []
    data = pd.read_csv('../processed_data/unbiased_with_region.tsv', sep='\t', header=0)
    for rid, region in enumerate(RegionName):
        df = data[data['InfectiousTime'] != "2019-07-01T00:00:00.000"]
        df = df[df['InfectiousTime'].notna()]
        agents = df[df['Region'] == region]['Agent'].values
        random.seed(0)
        random.shuffle(agents)
        unreport_rate = (1-rate2) if rid < 3 else (1-rate1)
        split_idx = int(len(agents) * unreport_rate)
        unreport_agent += list(agents[:split_idx])

    unreport_agent.sort()
    output_fn = '../processed_data/biased_%.2f_%.2f.tsv'

    data.loc[data['Agent'].isin(unreport_agent), 'InfectiousTime'] = np.nan
    data.to_csv(output_fn % (rate1, rate2), sep="\t", index=False)


def create_table_like_data():
    split_origin_into_regions()
    fn = '../processed_data/unbiased_with_region.tsv'
    report_with_rates(fn, 0.2, 0.2)
    for i in range(8):
        report_with_rates(fn, 0.2 + 0.1*(i+1) , 0.2 - 0.02 * (i+1))


# Convert string like time stamps to step numbers
def parseTimeToNumStep(timestamp):
    date, time = timestamp.split('T')

    month, day = date.split('-')[1:]
    hour, minute = time.split(':')[:2]

    month = int(month)
    day = int(day)
    hour = int(hour)
    minute = int(minute)

    dmonth = month - 7
    dday = dmonth * 31 + day - 1
    dminute = hour * 60 + minute
    numStep = dday * 288 + dminute // 5

    return numStep


def plot_cumulative(data_fn):
    data = pd.read_csv('../processed_data/%s'%data_fn, sep='\t', header=0)
    step = []
    for timeStamp in data['InfectiousTime']:
        if pd.isna(timeStamp):
            step.append(-1)
        else:
            step.append(parseTimeToNumStep(timeStamp))
    data['InfectiousStep'] = step

    fig, ax = plt.subplots(figsize=(5, 3.5), dpi=200)
    xaxis = np.arange(0,60,1/288)

    for region in RegionName:
        df = data[data['Region'] == region]
        countInfected = np.zeros(MaxStep)
        for step in df['InfectiousStep']:
            if step == -1 or step == 0:  # skip healthy & initial patients
                continue
            countInfected[step:] += 1

        ax.plot(xaxis, countInfected, label='Region ' + region, color=RegionColor[region])
    ax.legend(loc = 'upper left')
    ax.set_xlabel('Day')

    ax.xaxis.set_ticks(np.arange(0, 60 + (1/288), 10))
    ax.set_ylim(-50, 580)
    ax.grid(linewidth=0.5)

    ax.set_ylabel('Number of Infected People')
    fig.savefig('../figures/' + data_fn.replace('.tsv','.pdf'), format="pdf", bbox_inches="tight")


def create_step_wise_data(data_fn):
    data = pd.read_csv(data_fn, sep='\t', header=0)
    region = []
    inf = []
    sus = []
    rec = []
    s = []
    for r in RegionName:
        df = data [data['Region'] == r]
        steps = []
        for time_inf, time_rec in zip(df['InfectiousTime'], df['RecoverTime']):
            tmp = []
            if pd.isna(time_inf):
                tmp.append(-1)
            else:
                tmp.append(parseTimeToNumStep(time_inf))

            if pd.isna(time_rec):
                tmp.append(MaxStep)
            else:
                tmp.append(parseTimeToNumStep(time_rec))
            steps.append(tmp)

        susceptible_seq = np.ones(MaxStep) * NumAgent
        infectious_seq = np.zeros(MaxStep)
        recovered_seq = np.zeros(MaxStep)
        for step_inf, step_rec in steps:
            if step_inf == -1 or step_inf == 0:
                continue

            infectious_seq[step_inf:step_rec] += 1
            recovered_seq[step_rec:] += 1
            susceptible_seq[step_inf:] -= 1

        for i in range(120):
            region.append(r)
            s.append(i * 144)
            inf.append(infectious_seq[i * 144])
            sus.append(susceptible_seq[i * 144])
            rec.append(recovered_seq[i * 144])
        region.append(r)
        s.append(120 * 144)
        inf.append(infectious_seq[-1])
        sus.append(susceptible_seq[-1])
        rec.append(recovered_seq[-1])

    reform_data = {'region': region, 'step': s, 'susceptible': sus, 'infectious': inf, 'recovered': rec}
    reform_data = pd.DataFrame.from_dict(reform_data)
    reform_data.to_csv(data_fn.replace('biased','biased_ts'), sep="\t", index=False)

if __name__ == '__main__':
    create_table_like_data()
    plot_cumulative('unbiased_with_region.tsv')
    plot_cumulative('biased_%.2f_%.2f.tsv'%(0.8,0.08))
    fn = '../processed_data/biased_%.2f_%.2f.tsv'
    create_step_wise_data('../processed_data/unbiased_with_region.tsv')
    create_step_wise_data(fn%(0.2, 0.2))
    for i in range(8):
        create_step_wise_data(fn%( 0.2 + 0.1 * (i + 1), 0.2 - 0.02 * (i + 1)))
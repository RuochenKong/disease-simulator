import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import matplotlib.colors as mcolors
import networkx as nx
from networkx.drawing.nx_agraph import graphviz_layout
import sys

disease_version = sys.argv[1]
log_dir = '../examples/logs/'
disease_df = pd.read_csv(log_dir + disease_version + '/DiseaseStatusChangeJournal.tsv', sep = '\t|,', engine='python')
disease_df = disease_df[disease_df['diseaseStatus'] == 'Infectious'].reset_index(drop = True)
disease_df['rid'] = disease_df['[regionId'].apply(lambda x: int(x[1:]))
disease_df = disease_df[['step', 'agentId', 'rid', 'diseaseSeq']].reset_index(drop = True)
cases_in_order = sorted(disease_df['diseaseSeq'].to_list())

def generate_custom_gradient(start_hex, end_hex, num_colors=90):
    """Generate num_colors equidistant colors from start_hex to end_hex in #RRGGBB format."""
    start_rgb = np.array(mcolors.to_rgb(start_hex))  # Convert hex to RGB (0-1 range)
    end_rgb = np.array(mcolors.to_rgb(end_hex))      # Convert hex to RGB (0-1 range)

    # Linearly interpolate between start and end RGB colors
    gradient_rgb = [start_rgb + (end_rgb - start_rgb) * i / (num_colors - 1) for i in range(num_colors)]

    # Convert RGB values back to HEX
    gradient_hex = [mcolors.to_hex(color) for color in gradient_rgb]

    return gradient_hex

# Customize start and end colors
start_color = "#1A3636"  # Red
end_color = "#D6BD98"    # Blue
# Generate the gradient colors
sample_colors = generate_custom_gradient(start_color, end_color, num_colors=90)


with open(log_dir + disease_version + '/disease_spread.edgelist', 'w+') as f:
    for seq in cases_in_order:
        iniv_seq = seq.split('.')
        if len(iniv_seq) == 1:
            print(iniv_seq)
            continue
        
        step_current = disease_df[disease_df['diseaseSeq'] == seq]['step'].item()
        step_prev = disease_df[disease_df['diseaseSeq'] == '.'.join(iniv_seq[:-1])]['step'].item()
        
        gap = (step_current - step_prev)/288.0
        f.write('%s %s %.4f\n'%(iniv_seq[-2], iniv_seq[-1], gap))

node_colors = {}
all_nodes = []
for seq in cases_in_order:
    node = seq.split('.')[-1]
    step_current = disease_df[disease_df['diseaseSeq'] == seq]['step'].item() 
    step_current = min(int(step_current/288),89)
    node_colors[node]=sample_colors[step_current]
    all_nodes.append(node)
        
# Create a sample graph with edge weights
G_v1 = nx.Graph()
G_v2 = nx.Graph()

for node in all_nodes:
    G_v1.add_node(node)
    G_v2.add_node(node)

# Read the edge list and add edges to the graph
with open(log_dir + disease_version + '/disease_spread.edgelist') as f:
    for line in f.readlines():
        src, des, length = line.split()
        G_v1.add_edge(src, des, len=float(length))
        G_v2.add_edge(src, des)


colors = [node_colors[node] for node in G_v1.nodes()]

fig, ax = plt.subplots(1, figsize=(15,15))
pos = graphviz_layout(G_v1,  prog='neato', args='-Goverlap=false')
nx.draw(G_v1, pos, ax, with_labels=False,node_color=colors, node_size=30)
nx.draw_networkx_edges(G_v1, pos, edge_color='#DDDDDD', width=1)
plt.margins(0)
fig.savefig(log_dir + disease_version + '/figures/spread_tree_v1.pdf', bbox_inches='tight', pad_inches=0, format = 'pdf')
plt.close(fig)

colors = [node_colors[node] for node in G_v2.nodes()]
fig, ax = plt.subplots(1, figsize=(15,3))
pos = graphviz_layout(G_v2, prog='dot')
nx.draw(G_v2, pos, ax, with_labels=False, node_color=colors,alpha = 0.85, node_size=100)
nx.draw_networkx_edges(G_v2, pos, edge_color='#DDDDDD',alpha = 0.5, width=1)
plt.margins(0)
fig.savefig(log_dir + disease_version + '/figures/spread_tree_v2.pdf', bbox_inches='tight', pad_inches=0, format = 'pdf')
plt.close(fig)

for i in range(1,4):
    biased_disease_df = pd.read_csv(log_dir + disease_version+'/multi-biases-sampled/v%d.csv'%i, sep='\t|,', engine="python")
    biased_disease_df = biased_disease_df[biased_disease_df['diseaseStatus'] == 'Infectious'].reset_index(drop = True)
    biased_disease_df['rid'] = biased_disease_df['[regionId'].apply(lambda x: int(x[1:]))
    biased_disease_df['diseaseSeq'] = biased_disease_df['diseaseSeq'].apply(lambda x: x.replace('?.',''))
    biased_disease_df = biased_disease_df[['step', 'agentId', 'rid', 'diseaseSeq']].reset_index(drop = True)
    biased_cases_in_order = sorted(biased_disease_df['diseaseSeq'])

    with open(log_dir + disease_version + '/multi-biases-sampled/v%d_spread.edgelist'%i, 'w+') as f:
        for seq in biased_cases_in_order:
            iniv_seq = seq.split('.')
            if len(iniv_seq) == 1: 
                f.write('%s %s 0\n'%(iniv_seq[0], iniv_seq[0]))
                continue
            step_current = biased_disease_df[biased_disease_df['diseaseSeq'] == seq]['step'].item()
            step_prev = biased_disease_df[biased_disease_df['diseaseSeq'] == '.'.join(iniv_seq[:-1])]['step'].item()

            gap = (step_current - step_prev)/288.0
            f.write('%s %s %.4f\n'%(iniv_seq[-2], iniv_seq[-1], gap))

    G_v1 = nx.Graph()
    G_v2 = nx.Graph()
    
    # Read the edge list and add edges to the graph
    with open(log_dir + disease_version + '/multi-biases-sampled/v%d_spread.edgelist'%i) as f:
        for line in f.readlines():
            src, des, length = line.split()
            if length == '0':
                G_v1.add_node(src)
                G_v2.add_node(src)
            else:
                G_v1.add_edge(src, des, len=float(length))
                G_v2.add_edge(src, des)
    
    colors = [node_colors[node] for node in G_v1.nodes()]
    
    fig, ax = plt.subplots(1, figsize=(15,15))
    pos = graphviz_layout(G_v1, prog='neato', args='-Goverlap=false')
    nx.draw(G_v1, pos, ax, with_labels=False,node_color=colors, node_size=30)
    nx.draw_networkx_edges(G_v1, pos, edge_color='#DDDDDD', width=1)
    plt.margins(0)
    fig.savefig(log_dir + disease_version + '/figures/multi-biases/spread_tree_v%d_1.pdf'%i, bbox_inches='tight', pad_inches=0, format = 'pdf')
    plt.close(fig)
    
    colors = [node_colors[node] for node in G_v2.nodes()]
    
    fig, ax = plt.subplots(1, figsize=(15,3))
    pos = graphviz_layout(G_v2, prog='dot')
    nx.draw(G_v2, pos, ax, with_labels=False, node_color=colors, alpha = 0.85, node_size=100)
    nx.draw_networkx_edges(G_v2, pos, edge_color='#DDDDDD',alpha = 0.5, width=1)
    plt.margins(0)
    fig.savefig(log_dir + disease_version + '/figures/multi-biases/spread_tree_v%d_2.pdf'%i, bbox_inches='tight', pad_inches=0, format = 'pdf')
    plt.close(fig)
    
bias_types = ['age','gender','income','race','edu']
for bt in bias_types:
    for i in range(1,5):
        biased_disease_df = pd.read_csv(log_dir + disease_version+'/single-bias-sampled/%s_v%d.csv'%(bt,i), sep='\t|,', engine="python")
        biased_disease_df = biased_disease_df[biased_disease_df['diseaseStatus'] == 'Infectious'].reset_index(drop = True)
        biased_disease_df['rid'] = biased_disease_df['[regionId'].apply(lambda x: int(x[1:]))
        biased_disease_df['diseaseSeq'] = biased_disease_df['diseaseSeq'].apply(lambda x: x.replace('?.',''))
        biased_disease_df = biased_disease_df[['step', 'agentId', 'rid', 'diseaseSeq']].reset_index(drop = True)
        biased_cases_in_order = sorted(biased_disease_df['diseaseSeq'])
    
        with open(log_dir + disease_version + '/single-bias-sampled/%s_v%d_spread.edgelist'%(bt,i), 'w+') as f:
            for seq in biased_cases_in_order:
                iniv_seq = seq.split('.')
                if len(iniv_seq) == 1:
                    f.write('%s %s 0\n'%(iniv_seq[0], iniv_seq[0]))
                    continue
                step_current = biased_disease_df[biased_disease_df['diseaseSeq'] == seq]['step'].item()
                step_prev = biased_disease_df[biased_disease_df['diseaseSeq'] == '.'.join(iniv_seq[:-1])]['step'].item()

                gap = (step_current - step_prev)/288.0
                f.write('%s %s %.4f\n'%(iniv_seq[-2], iniv_seq[-1], gap))
                
        G_v1 = nx.Graph()
        G_v2 = nx.Graph()
    
        # Read the edge list and add edges to the graph
        with open(log_dir + disease_version + '/single-bias-sampled/%s_v%d_spread.edgelist'%(bt,i)) as f:
            for line in f.readlines():
                src, des, length = line.split()
                if length == '0':
                    G_v1.add_node(src)
                    G_v2.add_node(src)
                else:
                    G_v1.add_edge(src, des, len=float(length))
                    G_v2.add_edge(src, des)
    
        colors = [node_colors[node] for node in G_v1.nodes()]
    
        fig, ax = plt.subplots(1, figsize=(15,15))
        pos = graphviz_layout(G_v1, prog='neato', args='-Goverlap=false')
        nx.draw(G_v1, pos, ax, with_labels=False,node_color=colors, node_size=30)
        nx.draw_networkx_edges(G_v1, pos, edge_color='#DDDDDD', width=1)
        plt.margins(0)
        fig.savefig(log_dir + disease_version + '/figures/single-bias/%s/spread_tree_v%d_1.pdf'%(bt,i), bbox_inches='tight', pad_inches=0, format = 'pdf')
        plt.close(fig)
    
    
        colors = [node_colors[node] for node in G_v2.nodes()]
    
        fig, ax = plt.subplots(1, figsize=(15,3))
        pos = graphviz_layout(G_v2, prog='dot')
        nx.draw(G_v2, pos, ax, with_labels=False, node_color=colors,alpha = 0.85, node_size=100)
        nx.draw_networkx_edges(G_v2, pos, edge_color='#DDDDDD',alpha = 0.5, width=1)
        plt.margins(0)
        fig.savefig(log_dir + disease_version + '/figures/single-bias/%s/spread_tree_v%d_2.pdf'%(bt,i), bbox_inches='tight', pad_inches=0, format = 'pdf')
        plt.close(fig)


# Overview

This folder contains all generated dataset and the visualization jupyter notebook 
for the paper ***"An Infectious Disease Spread Simulation to Control Data Bias"*** currently under review.

## Available Datasets

Currently, 10 datasets are available stored separately in each folder. The details are as following:
- `atl-50`,`atl-60`,`atl-70`, and `atl-80`: These datasets used the map of Atlanta downtown run with 5000 agents, as set in [atl.5k.properties](../atl.5k.properties).  
    To test the effectiveness of simulating census data and single bias model, 
    we did an experiment by binary split the characteristics values into two groups. (i.e. age over/under 50, annual individual income over/under 70k, race white/not white, and male/female). We assigned the reporting chances for each two groups with 50/50, 60/40, 70/30, and 80/20, resulting in the four different datasets. 
    The corresponding files for setting up the reporting chance are in [bias.50.single](../bias.50.single), [bias.60.single](../bias.60.single), [bias.70.single](../bias.70.single), and [bias.80.single](../bias.80.single).
    The parameters for multivariate bias model is in [bias.properties](../bias.properties), which is the same as the default values when running with GUI. 
    The experiment is reproducible with four batch files in [run_atl_50.sh](../run_atl_50.sh), [run_atl_60.sh](../run_atl_60.sh), [run_atl_70.sh](../run_atl_70.sh), and [run_atl_80.sh](../run_atl_80.sh).
- `sf-50`,`sf-60`,`sf-70`, and `sf-80`: These datasets used the map of partial San-Francisco run with 5000 agents, as set in [sanfran.5k.properties](../sanfran.5k.properties). 
    We did the same experiment on the San Francisco map. The parameters for the single bias model and multivariate bias model are the same as above.
    The experiment is also reproducible with four batch files in [run_sanfran_50.sh](../run_sanfran_50.sh), [run_sanfran_60.sh](../run_sanfran_60.sh), [run_sanfran_70.sh](../run_sanfran_70.sh), and [run_sanfran_80.sh](../run_sanfran_80.sh).
- `atlanta` and `sanfran`: These datasets are from the simulation with default single and multivariate bias models. Both of them used 2000 agents. The related configuration files are [atlanta.properties](../atlanta.properties), [sanfran.properties](../sanfran.properties), [bias.single.properties](../bias.single.properties), and [bias.properties](../bias.properties).
    The related batch files are [run_atlanta.sh](../run_atlanta.sh) and [run_san_francisco.sh](../run_san_francisco.sh).

The maps for Atlanta and San Francisco are in [atlanta](../atlanta) and [san-fran](../san-fran).

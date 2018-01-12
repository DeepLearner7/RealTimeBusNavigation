import pandas as pd
import numpy as np
import matplotlib.pyplot as plt, mpld3
import sys

df = pd.read_csv('Crowd.csv',index_col=0)
n = str(sys.argv[1])

found=False
for i in range(0,100):
    if df.iloc[i,0] == n:
        x=df.loc[i]
        found=True
        break

if found==False:
    print('NO SUCH BUS EXISTS')
    exit()
    
x = x.drop(x.index[0])
x = x[1:]
y_pos = np.arange(len(x))

fig = plt.figure(1, [7,5])
ax = fig.gca()
ax.bar(y_pos,x, align='center', alpha=0.9, width=0.6)
plt.xticks(y_pos, df.columns[2:])
plt.ylabel('Crowd')
plt.xlabel('Stops')

mpld3.save_html(fig,"graph.html")

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt, mpld3
import sys

df = pd.read_csv('timing.csv',index_col=0)
n = str(sys.argv[1])

found=False
for i in range(0,100):
    if df.index[i] == n:
        x=df.iloc[i]
        found=True
        break

if found==False:
    print('NO SUCH BUS EXISTS')
    exit()
    
x = x[0:]
y_pos = np.arange(len(x))

fig = plt.figure(1, [7,5])
ax = fig.gca()  
ax.set_ylim([-5,7])
ax.scatter(y_pos,x, alpha=0.9)
ax.plot(y_pos, x, alpha=0.5)
ax.plot([0,30], [0,0], color = 'red')
plt.ylabel('Time Deviation')
plt.xlabel('Days')

mpld3.save_html(fig,"scatterPlot.html")

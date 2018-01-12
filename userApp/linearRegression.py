import pandas as pd
import numpy as np
import sys
import matplotlib.pyplot as plt, mpld3
from sklearn import preprocessing
from sklearn.linear_model import LinearRegression
from sklearn import cross_validation
from matplotlib import style
style.use('ggplot')
target = '1'

n = "8826"
n = n + ".csv"

df = pd.read_csv(n)
df.drop("Unnamed: 0",1,inplace=True)

Y = np.array(df[target])
X = np.array(df.index)

#Taking 75% of Scaled_X and Scaled_Y for training and rest 25% for testing purpose
X_train , X_test ,  Y_train , Y_test = cross_validation.train_test_split(X,Y,test_size=0.25)


X_train = X_train.reshape(X_train.shape[0], 1)
X_test = X_test.reshape(X_test.shape[0], 1)

clf = LinearRegression()
acc = clf.fit(X_train,Y_train)  #Training

fig = plt.figure( figsize=(10, 7))
plt.scatter(X,Y, alpha = 0.9)
plt.plot(X_train,clf.predict(X_train), c='g', alpha = 0.5)
plt.xlabel('Days')
plt.ylabel('Crowd Distribution')
plt.show()

mpld3.save_html(fig,"crowdPrediction.html")


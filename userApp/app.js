var express = require('express');
var bodyParser = require('body-parser');
var path = require('path');
var app = express();
var firebase = require("firebase");
var Promises = require("bluebird");
var fs = require('fs');
var https = require('https');
var PythonShell = require('python-shell');

var options = {
 key: fs.readFileSync('key.pem'),
 cert: fs.readFileSync('cert.pem')
};

var recv_data;
var config = {
apiKey: "AIzaSyDwe6WI2G-CjEIBzXYbEduA3IA-xWtaKfs",
authDomain: "rtbn-d0cde.firebaseapp.com",
databaseURL: "https://rtbn-d0cde.firebaseio.com/",
storageBucket: "rtbn-d0cde.appspot.com",
};

app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

firebase.initializeApp(config);
db = firebase.database();


app.use("/assets/js",  express.static(__dirname + '/assets/js'));
app.use("/assets/css", express.static(__dirname + '/assets/css'));
app.use("/assets/fonts",  express.static(__dirname + '/assets/fonts'));
app.use("/images",  express.static(__dirname + '/images'));

app.get('/', function (req, res) {
    res.sendFile('index.html', { root: __dirname  } );

});

app.get('/bus_info', function (req, res) {
    res.sendFile('bus_info.html', { root: __dirname  } );

});
app.get('/graph', function (req, res) {
    res.sendFile('graph.html', { root: __dirname  } );

});
app.get('/scatterPlot', function (req, res) {
    res.sendFile('scatterPlot.html', { root: __dirname  } );

});

app.get('/crowdPrediction', function(req, res){
    res.sendFile('crowdPrediction.html', {root: __dirname });
});

var nearby = [], lastVisited = [];
var src_stop, dest_stop;
app.post('/', function(request, response){
    src_lat = request.body['src_lat'];
    src_lng = request.body['src_lng'];
    dest_lat = request.body['dest_lat'];
    dest_lng = request.body['dest_lng'];

    var curr_dist_src, curr_dist_dest, min_dist_src = 1000.0, min_dist_dest = 1000.0;
    return new Promises(function(resolve,reject){
        var ref = db.ref("stops_coordinate");
        ref.once('value',function(result){
            if(result.val() === undefined){
                console.log("undefined data");
                reject();
            }
            else{
                resolve(result.val());
            }
        }).then((result)=>{
            get_master = result.toJSON();
            return get_master;
        }).then((result)=>{
            for(var key in result){
                if(result.hasOwnProperty(key)){
                    var str = JSON.stringify(result[key]);
                    var str1 = "", str2 = "";
                    var flag = 1;
                    for(var i=1; i<str.length; i++){
                        if(flag == 1 && str[i] != " ") 
                            str1 += str[i]
                        else{
                            if(flag)
                                i++;
                            flag = 0;
                            str2 += str[i];
                        }
                    }
                    var x = parseFloat(str1);
                    var y = parseFloat(str2);


                    curr_dist_src = getDistance(x, y, src_lat, src_lng);
                    curr_dist_dest = getDistance(x, y, dest_lat, dest_lng);

                    if(curr_dist_src < min_dist_src){
                        min_dist_src = curr_dist_src;
                        src_stop = key;
                    }

                    if(curr_dist_dest < min_dist_dest){
                        min_dist_dest = curr_dist_dest;
                        dest_stop = key;
                    }

                }
            }

            console.log(src_stop);
            console.log(dest_stop);
            var data = {
                "src_stop" : src_stop,
                "dest_stop" : dest_stop
            }
            response.send(data);

        })
    })
});

app.post('/findBuses',function(req, res){
    var get_bus_data;
    return new Promises(function(resolve,reject){
        var ref = db.ref("CurrentLocation");
        ref.once('value', function(result){
            if(result.val() === undefined)
            {
                console.log("undefined data");
                reject();
            }
            else
            {
                resolve(result.val());
            }
        }).then((result)=>{
            get_bus_data = result.toJSON();
            return get_bus_data;
        }).then((result)=>{
            //response.send(result);
            var str = JSON.stringify(result);
            var arr = str.split(/[{'" ,:"'}]+/);
            nearby.length = 0;
            for(var j = 1; j< arr.length-1; j++){
                var id = arr[j++];
                var Lat = arr[j];
                var x = parseFloat(arr[j++]);
                var Lng = arr[j];
                var y = parseFloat(arr[j]);

                //console.log(x + " " + y);

                //console.log(getDistance(lat, lng, x, y));
                if( getDistance(src_lat, src_lng, x, y) <= 5){
                    nearby.push(id);
                    nearby.push(Lat);
                    nearby.push(Lng);
                }
            }
            res.send(nearby);            
        })
    })

});

app.post('/findDetails', function(request, response){
    var id = (request.body['id']);
    var bus_stops = [];
    var ref = db.ref("BusRoutes/" + id);
    ref.once('value', function(result){
        result = result.toJSON();
        var str = JSON.stringify(result);
        var arr = str.split(/[{'":,"'}]/);
        var data = [];

        bus_stops.length = 0;
        
        for(var i=0; i<arr.length; i++){
            if(arr[i] != '')
                data.push(arr[i]);
        }
        // console.log("1.1>");
        // console.log(data);
        for(var i=2; i<data.length; i+=3){
            bus_stops.push(data[i]);
        }
        
        var lv;
        var lref = db.ref("lastVisited/" + id);
        lref.once('value', function(result){
            lv = JSON.stringify(result.toJSON());
            var l = lv.toString();

            var flag = 0, flag2 = 0, onway = 0;
            for(var i=0; i<bus_stops.length; i++) {
                var m = '"' + bus_stops[i].toString() + '"';
                if( m === l ) {
                    //console.log("mil gaya " + bus_stops[i]);
                    flag = 1;
                }   
                else if( flag == 1 && ( bus_stops[i].toString() === src_stop.toString() ) ) {
                    //console.log("source bhi mil gaya " + bus_stops[i]);
                    flag2 = 1;
                }
                else if( flag2==1 && flag == 1 && ( bus_stops[i].toString() === dest_stop.toString() ) ) {
                    //console.log("source bhi mil gaya " + bus_stops[i]);
                    onway = 1;
                }
            }

            if(onway){
                var send = {
                    "status" : "On your way"
                }
            }
            else{
                var send = {
                    "status" : "Not on your way"
                }
            }
            //console.log(send);
            response.send(send);
        });
    })

});

var bus_stops = [],coordinates = [];
app.post('/bus_info', function(request, response){
    var id = request.body['bus_id'];
    var get_route_data;
    return new Promises(function(resolve,reject){
        var ref = db.ref("BusRoutes/" + id);
        ref.once('value', function(result){
            if(result.val() === undefined){
                console.log("undefined data");
                reject();
            }
            else{
                resolve(result.val());
            }
        }).then((result)=>{
            get_route_data = result.toJSON();
            return get_route_data;
        }).then((result)=>{
            //response.send(result);
            var str = JSON.stringify(result);
            var arr = str.split(/[{'":,"'}]/);
            var data = [];

            bus_stops.length = 0;
            coordinates.length = 0;

            for(var i=0; i<arr.length; i++){
                if(arr[i] != '')
                    data.push(arr[i]);
            }
            for(var i=2; i<data.length; i+=3){
                bus_stops.push(data[i]);
            }
            for(var i=1; i<data.length; i+=3){
                coordinates.push(data[i]);
            }
            console.log(bus_stops);
            console.log(coordinates);

            //Bar-chart starts
            var options = {
                args: [id]
            };
            /*
            PythonShell.run('linearRegression.py', options, function (err) {
                if (err) throw err;
                console.log('linearRegression script executed');
            });*/
            PythonShell.run('histogram.py', options, function (err) {
                if (err) throw err;
                console.log('bar-graph script executed');
            });                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
            //Bar-chart ends
            PythonShell.run('scatterPlot.py', options, function (err) {
                if (err) throw err;
                console.log('scatter-plot script executed');
            });

            var res = {
                "bus_id" : id,
                "bus_stops" : bus_stops,
                "coordinates" : coordinates
            }
            //console.log(res);
            response.send(res);

        })
    }) 
});




https.createServer(options, app).listen(3000, function () {
  console.log('Started!');
});

function getDistance(lat1, lon1, lat2, lon2){
    var R = 6371;
    var dLat = deg2rad(lat2-lat1);
    var dLon = deg2rad(lon2-lon1);
    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
            Math.sin(dLon/2) * Math.sin(dLon/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    var d = R * c;
    return d;
}

function deg2rad(deg){
    return deg * (Math.PI/180)
}

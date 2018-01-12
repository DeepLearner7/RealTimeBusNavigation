var firebase = require("firebase");
var recv_data;
var config = {
 apiKey: "AIzaSyAPQTbXzEk-06VADNsxSQ6i0OXZeDyh3Jo",
 authDomain: "realtimebusnavigationsystem.firebaseapp.com",
 databaseURL: "https://realtimebusnavigationsystem.firebaseio.com",
 storageBucket: "realtimebusnavigationsystem.appspot.com",
};
firebase.initializeApp(config);
db = firebase.database();
// Get a database reference to our posts



module.exports = function get_bus_data(lat) {

    //var ref = db.ref("message");

    // Attach an asynchronous callback to read the data at our posts reference
    //ref.on('value', gotData, errData);

    function gotData(data){
       //console.log(data.val());
       recv_data = data.val();
       return recv_data;
    }


    function errData(err){
       console.log('Error!');
       console.log(err);
    }

    return db.ref("message").on('value', gotData, errData);
}
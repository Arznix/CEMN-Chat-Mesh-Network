/////////////////////////////////////////////////////////
//-------------------------------------------------------
//-------------------------------------------------------
//-------------Server.js---------------------------------
//-------------------------------------------------------
//------------Milad-Hajihassan---------------------------
//-------------------------------------------------------
/////////////////////////////////////////////////////////

// set up 
var express  = require('express');
var bodyParser = require('body-parser');
var xmlparser = require('express-xml-bodyparser');
var port     = process.env.PORT || 8081;
// create an instance of express
const app = express();

app.use(bodyParser.text({type: '*/*'}));
/*app.use(bodyParser.urlencoded({ extended: true }))
   .use(bodyParser.json());
   */

//app.use(xmlparser());

// connection to the database
require('./configs/database');

// route registration
app.use('/api', require('./routes/user'));

// error handling

// 404 errors
app.use((req, res, next) => {
  res.status(404).json({ message: 'Resource not found' });
});

// 500 errors
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ message: err.message });
});



// start the server
const server = app.listen(port, () => {
  console.log(`App is running at: localhost:${server.address().port}`);
});
'use strict';

const mongoose = require('mongoose');
const databaseName = 'cemn_user';

// connect to the database
mongoose.Promise = global.Promise;
mongoose.connect(`mongodb://localhost:27017/${databaseName}`);

const connection = mongoose.connection;

connection.on('error', console.error.bind(console, 'connection error:'));
connection.on('open', () => {
  console.log(`Connected to the ${databaseName} database`);
});
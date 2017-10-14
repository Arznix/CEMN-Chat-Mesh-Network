'use strict';

const mongoose = require('mongoose');

const schema = new mongoose.Schema({
  username: { type: String, required: [true, 'username is required'] },
  password: { type: String, required: [true, 'password is required'] },
  publicKey: { type: String, required: [true, 'public key is required'] },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('users', schema);
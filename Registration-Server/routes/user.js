'use strict';

// dependencies
const express = require('express');
const User = require('../models/user');
var parseString = require('xml2js').parseString;
var crypto = require('crypto');
var cryptoFile = require('../configs/crypto');
// instance of express router
const router = express.Router();


function encrypt(text){
  var cipher = crypto.createCipher(cryptoFile.credentials.algorithm,cryptoFile.credentials.password)
  var crypted = cipher.update(text,'utf8','hex')
  crypted += cipher.final('hex');
  return crypted;
}

function decrypt(text){
  var decipher = crypto.createDecipher(cryptoFile.credentials.algorithm,cryptoFile.credentials.password)
  var dec = decipher.update(text,'hex','utf8')
  dec += decipher.final('utf8');
  return dec;
}

// routes ending with /users
router.route('/users')
  .post((req, res) => {
	  parseString(decrypt(req.body), function (err, result) {

			var req_xml = result["data"];
			//console.log(req_xml);
			//Create new user
			if(req_xml["cmd"]=="new")
			  {
				  	if ((!req_xml["user_name"]) || (!req_xml["pass_word"]) || (!req_xml["public_key"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}	
					else
					{
						User.findOne({username: req_xml["user_name"]}, function(err, userItem) {
							if (err) {
								return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
							}
							if (!userItem) {
								var insertUser = {username: req_xml["user_name"],password: req_xml["pass_word"], publicKey: req_xml["public_key"]};
								User.create(insertUser, function(err) {
										if (err) {
											return res.send(err);
										}
										return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>new_user:'+ req_xml["user_name"] +'</message></data>'));
									});
							}
							else {
								return res.send(encrypt('<data><cmd>response</cmd><error>3</error></data>'));
							}
						});
					}

			}
			//Retrieve Public key
			else if(req_xml["cmd"]=="retrieve_public_key")
			  {
					if ((!req_xml["requester_user_name"]) || (!req_xml["pass_word"]) || (!req_xml["requested_user_name"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}	
					else 
					{
						var queryRequester = {username: req_xml["requester_user_name"],password: req_xml["pass_word"]};
						var queryRequested = {username: req_xml["requested_user_name"]};
						User.findOne(queryRequester, (err, requesterUserItem) => {
						  if (err){
							return res.send(err);
						  }
						  
							if (requesterUserItem) {
								  User.findOne(queryRequested, (err, requestedUserItem) => {
								  if (err){
									return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
								  }
								  return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>retrieved_public_key:'+ requestedUserItem.publicKey +'</message></data>'));
								});
							}
							else {
								return res.send(encrypt('<data><cmd>response</cmd><error>3</error></data>'));
							}
						});					
					}

			  }	
			  //change username
			  else if(req_xml["cmd"]=="change_user_name")
			  {
				  	if ((!req_xml["old_user_name"]) || (!req_xml["pass_word"]) || (!req_xml["new_user_name"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}	
					else
					{
						var query = {username: req_xml["old_user_name"],password: req_xml["pass_word"]};
						var update = {username: req_xml["new_user_name"]};
						var options = {new: true};
						User.findOneAndUpdate(query, update, options, function(err, userItem) {	
						  if (err){
							return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
						  }
						  return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>changed_user_name:'+ userItem.username +'</message></data>'));
						});						
					}

			  }
			  //change password
			  else if(req_xml["cmd"]=="change_pass_word")
			  {
				  	if ((!req_xml["user_name"]) || (!req_xml["current_pass_word"]) || (!req_xml["new_pass_word"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}
					else
					{
						var query = {username: req_xml["user_name"],password: req_xml["current_pass_word"]};
						var update = {password: req_xml["new_pass_word"]};
						var options = {new: true};
						User.findOneAndUpdate(query, update, options, function(err, userItem) {	
						  if (err){
							return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
						  }
						  return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>changed_pass_word:'+ userItem.password +'</message></data>'));
						});								
					}
  
			  }
			  //change publicKey
				else if(req_xml["cmd"]=="change_public_key")
			  {
				 
				  	if ((!req_xml["user_name"]) || (!req_xml["pass_word"]) || (!req_xml["new_public_key"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}
					else
					{
						var query = {username: req_xml["user_name"],password: req_xml["pass_word"]};
						var update = {publicKey: req_xml["new_public_key"]};
						var options = {new: true};
						User.findOneAndUpdate(query, update, options, function(err, userItem) {	
						  if (err){
							return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
						  }
						  return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>changed_public_key:'+ userItem.publicKey +'</message></data>'));
						  //return res.json({ message: 'changed_public_key:' + userItem.publicKey });
						});							
					}
		  
			  }	
			  //delete user
				else if(req_xml["cmd"]=="delete_user")
			  {
				    if ((!req_xml["user_name"]) || (!req_xml["pass_word"])) 
					{
						return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
					}	
					else{
						var query = {username: req_xml["user_name"],password: req_xml["pass_word"]};
						User.findOneAndRemove(query, (err,userItem) => {
						  if (err){
							return res.send(encrypt('<data><cmd>response</cmd><error>2</error></data>'));
						  }
						  if (userItem) {
							return res.send(encrypt('<data><cmd>response</cmd><error>0</error><message>deleted_user:'+ req_xml["user_name"] +'</message></data>'));
							//return res.json({  message: 'deleted_user:' + req_xml["user_name"] });
						  }
						  else{
							  return res.send(encrypt('<data><cmd>response</cmd><error>1</error></data>'));
						  }
						});		
					}
  
			  }	
			  else{
				  //return res.json({ message: 'cmd:0'});
				  return res.send(encrypt('<data><cmd>response</cmd><error>4</error></data>'));
			  }
		});
  });



module.exports = router;
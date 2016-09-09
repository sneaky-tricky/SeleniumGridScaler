/**
* A sample Lambda function that looks up the latest AMI ID for a given service_id and filters.
**/

var aws = require("aws-sdk");
 
exports.handler = function(event, context) {
 
    console.log("REQUEST RECEIVED:\n" + JSON.stringify(event));
    
    // For Delete requests, immediately send a SUCCESS response.
    if (event.RequestType == "Delete") {
        sendResponse(event, context, "SUCCESS");
        return;
    }
 
    var responseStatus = "FAILED";
    var responseData = {};
 
    var ec2 = new aws.EC2({region: event.ResourceProperties.Region});
    var filters = getFilters(event);
    console.log("Looking for filters: " + filters);
    var describeImagesParams = {
        Filters: filters
    };
    console.log("executing ec2.describeImages " + describeImagesParams );
    // Get AMI IDs with the specified name pattern and owner
    ec2.describeImages(describeImagesParams, function(err, describeImagesResult) {
        if (err) {
            responseData = {Error: "DescribeImages call failed"};
            console.log(responseData.Error + ":\n", err);
        }
        else {
            console.log("executed ec2.describeImages. Have  " + describeImagesResult.Images.length + " image." );
            var images = describeImagesResult.Images;
            // Sort images by name in decscending order. The names contain the AMI version, formatted as YYYY.MM.Ver.
            images.sort(function(x, y) { return y.CreationDate.localeCompare(x.CreationDate); });
            for (var j = 0; j < images.length; j++) {
                console.log("checking image AmiId:" + images[j].ImageId + " Name:" + images[j].Name)
                if (images[j].State != "available") { 
                    continue;
                }
                responseStatus = "SUCCESS";
                responseData["Id"] = images[j].ImageId;
                break;
            }
        }
        sendResponse(event, context, responseStatus, responseData);
    });
};

function getFilters(event) {
    var ret = [];
    var serviceId = event.ResourceProperties.ServiceId;
    if (serviceId) {
        ret.push({ Name: "tag:service_id", Values: [serviceId]});
    }
    try {
        var extraFilterNames = event.ResourceProperties.FilterNames;
        if (extraFilterNames) {
        var extraFilterValues = event.ResourceProperties.FilterValues;
            for (var i = 0; i < extraFilterNames.length; i++) {
                ret.push({ Name: extraFilterNames[i], Values: [extraFilterValues[i]]});
            }
        }
    } catch(e) {
        console.log("Error parsing filters: " + e);
    }
     return ret;
}

// Check if the image is a beta or rc image. The Lambda function won't return any of those images.
function isBeta(imageName) {
    return imageName.toLowerCase().indexOf("beta") > -1 || imageName.toLowerCase().indexOf(".rc") > -1;
}


// Send response to the pre-signed S3 URL 
function sendResponse(event, context, responseStatus, responseData) {
 
    var responseBody = JSON.stringify({
        Status: responseStatus,
        Reason: "See the details in CloudWatch Log Stream: " + context.logStreamName,
        PhysicalResourceId: context.logStreamName,
        StackId: event.StackId,
        RequestId: event.RequestId,
        LogicalResourceId: event.LogicalResourceId,
        Data: responseData
    });
 
    console.log("RESPONSE BODY:\n", responseBody);
 
    var https = require("https");
    var url = require("url");
 
    var parsedUrl = url.parse(event.ResponseURL);
    var options = {
        hostname: parsedUrl.hostname,
        port: 443,
        path: parsedUrl.path,
        method: "PUT",
        headers: {
            "content-type": "",
            "content-length": responseBody.length
        }
    };
 
    console.log("SENDING RESPONSE...\n");
 
    var request = https.request(options, function(response) {
        console.log("STATUS: " + response.statusCode);
        console.log("HEADERS: " + JSON.stringify(response.headers));
        // Tell AWS Lambda that the function execution is done  
        context.done();
    });
 
    request.on("error", function(error) {
        console.log("sendResponse Error:" + error);
        // Tell AWS Lambda that the function execution is done  
        context.done();
    });
  
    // write data to request body
    request.write(responseBody);
    request.end();
}
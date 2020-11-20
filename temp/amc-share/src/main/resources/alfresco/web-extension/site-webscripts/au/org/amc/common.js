function packageInResponseJSON(success, responseCode, responseMessage, payLoad) {
	var response = {};
	response.responseCode = responseCode;
	response.responseMessage = responseMessage;
	response.payLoad = payLoad;
	response.success = success;
	return jsonUtils.toJSONString(response);
}
var exec = require('cordova/exec');

module.exports = {
    
    Type: {
        IMAGE_SINGLE: 1,
        IMAGE_MULTI: 2,
        VIDEO_SINGLE: 3,
        VIDEO_MULTI: 4,
        MIX: 5
    },

    Scene: {
        EDIT: 0,
        PUBLISH: 1
    },
    
    isInstalled: function (onSuccess, onError) {
        exec(onSuccess, onError, "Douyin", "isDouyinAppInstalled", []);
    },

    /**
     * Share a message to wechat app
     *
     * @example
     * <code>
     * Douyin.share({
     *    message: {
     *    title: "Hi, there",
     *   description: "This is description.",
     *   imageUrl: "www/img/thumbnail.png",
     *   webpageUrl: "http://tech.qq.com/zt2012/tmtdecode/252.htm"
		    },
     *    },
     *  var scope = "user_info",
     * </code>
     */
    share: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Douyin", "share", [message]);
    },

    /**
     * Sending an auth request to Douyin
     *
     * @example
     * <code>
	 *	Douyin.auth(scope, function (response) {
	 *  // you may use response.code to get the access token.
	 *   alert(JSON.stringify(response));
	 *	}, function (reason) {
	 *   alert("Failed: " + reason);
	 *	});

     * </code>
     */
    auth: function (scope, onSuccess, onError) {
        if (typeof scope == "function") {
            return exec(scope, "Douyin", "sendAuthRequest");
        }

        return exec(onSuccess, onError, "Douyin", "sendAuthRequest", [scope]);
    },

    
    jumpToDouyin: function (url, onSuccess, onError) {
        exec(onSuccess, onError, "Douyin", "jumpToDouyin", [url]);
    }

};


# cordova-plugin-douyin

A cordova plugin, a JS version of Douyin SDK


# Feature

Share title, description, image, and link to douyin，choose invoice from douyin list


# Install

1. ```ionic cordova plugin add cordova-plugin-jsaonhe-douyin  --variable douyinappid=YOUR_DOUYIN_APPID```, 
2. ```cordova build ios``` or ```cordova build android```

3. (iOS only) if your cordova version <5.1.1,check the URL Type using XCode

# Usage

## Check if wechat is installed
```Javascript
Douyin.isInstalled(function (installed) {
    alert("Douyin installed: " + (installed ? "Yes" : "No"));
}, function (reason) {
    alert("Failed: " + reason);
});
```

## Authenticate using Wechat
```Javascript
var scope = "user_info",
Douyin.auth(scope, function (response) {
    // you may use response.code to get the access token.
    alert(JSON.stringify(response));
}, function (reason) {
    alert("Failed: " + reason);
});
```

## Share media(e.g. link)
```Javascript
Douyin.share({
    message: {
        title: "Hi, there",
        imageUrls: ["www/img/thumbnail.png","www/img/thumbnail.png"],
    },
    shareType: 0
}, function () {
    alert("Success");
}, function (reason) {
    alert("Failed: " + reason);
});
```

//
//  CDVKwais.m
//  cordova-plugin-jasonhe-kwais
//
//
//

#import "CDVDouyin.h"
#import <Photos/Photos.h>
static int const MAX_THUMBNAIL_SIZE = 320;

@implementation CDVDouyin

#pragma mark "API"
- (void)pluginInitialize {
    self.douyinAppId = [[self.commandDelegate settings] objectForKey:@"douyinappid"];
//    self.universalLink  = [[self.commandDelegate settings] objectForKey:@"universallink"];
    NSLog(@"cordova-plugin-jasonhe-douyin SDK APP_ID: %@",self.douyinAppId);
    
    NSString *appid = [[DouyinOpenSDKApplicationDelegate sharedInstance] appId];
    if (!appid) {
        
        if (self.douyinAppId && [[DouyinOpenSDKApplicationDelegate sharedInstance] registerAppId:self.douyinAppId]) {
            NSLog(@"cordova-plugin-jasonhe-douyin SDK 主动注册成功！/n");
        } else {
            NSLog(@"cordova-plugin-jasonhe-douyin SDK 主动注册失败！/n");
        }
    } else {
        NSLog(@"cordova-plugin-jasonhe-douyin SDK 主动注册失败！/n");

    }
}

- (void)isDouyinAppInstalled:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[[DouyinOpenSDKApplicationDelegate sharedInstance] isAppInstalled]];

            [self.commandDelegate sendPluginResult:commandResult callbackId:command.callbackId];
        });
        
    }];
    
    
}

- (void)share:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        // if not installed
        dispatch_async(dispatch_get_main_queue(), ^{
            if (![[DouyinOpenSDKApplicationDelegate sharedInstance] isAppInstalled])
            {
                [self failWithCallbackID:command.callbackId withMessage:@"未安装抖音"];
                return ;
            }
            
            // check arguments
            NSDictionary *params = [command.arguments objectAtIndex:0];
            if (!params)
            {
                [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
                return ;
            }

            // save the callback id
            self.currentCallbackId = command.callbackId;
            NSDictionary *message = [params objectForKey:@"message"];

            if (message)
            {
                
                NSInteger mediaType = 0;
                if ([params objectForKey:@"shareType"]) {
                    mediaType = (int)[[params objectForKey:@"shareType"] integerValue];
                }

                mediaType = 1;
                if (mediaType == 0) {
                    [self getImageIdsWithImageUrls:[message objectForKey:@"imageUrls"] doneBlock:^(NSArray *ids) {
                   
                        if (!ids.count) {
                            [self failWithCallbackID:command.callbackId withMessage:@"图片保存出错了"];
                            return;
                        }
                        
                        DouyinOpenSDKShareRequest *req = [[DouyinOpenSDKShareRequest alloc] init];
                        req.mediaType =  DouyinOpenSDKShareMediaTypeImage;
                        req.localIdentifiers = ids;//NSArray
                        
                        req.hashtag  = [message objectForKey:@"title"];
                    
                        dispatch_async(dispatch_get_main_queue(), ^{
                          BOOL succes = [req sendShareRequestWithCompleteBlock:^(DouyinOpenSDKShareResponse * _Nonnull respond) {


                            if (respond.isSucceed) {

                            // Share Succeed
                                self.currentCallbackId = command.callbackId;
                                [self successWithCallbackID:self.currentCallbackId];


                            } else{
                                NSLog(@"share error code:%@",@(respond.shareState) );
                                [self failWithCallbackID:command.callbackId withMessage:@"发送请求失败"];
                                
                                self.currentCallbackId = nil;

                            }

                            }];
                            if (succes) {

                            } else{


                            }
                        });
                            
                    }];
                } else {
                    // video
                    self.command = command;
                    self.shareDic = message;
                    [self getVideosIdsWithUrls:[message objectForKey:@"imageUrls"]];
                }
               
               
            } else {
                
                self.currentCallbackId = nil;
            }
        });
        

       
    }];
    
    
}

- (void)sendAuthRequest:(CDVInvokedUrlCommand *)command
{

//    KSAuthRequest *req = [[KSAuthRequest alloc] init];
//
//    // scope
//    if ([command.arguments count] > 0)
//    {
//        req.scope = [command.arguments objectAtIndex:0];
//    }
//    else
//    {
//        req.scope = @"user_info";
//    }
//
//    req.h5AuthViewController = self.viewController;
//
//    [KSApi sendRequest:req completion:^(BOOL success) {
//        if (success) {
//            // save the callback id
//            self.currentCallbackId = command.callbackId;
//        } else {
//            [self failWithCallbackID:command.callbackId withMessage:@"发送请求失败"];
//
//        }
//    }];
}



- (void)jumpToDouyin:(CDVInvokedUrlCommand *)command
{
    // check arguments
    NSString *url = [command.arguments objectAtIndex:0];
    if (!url || ![url hasPrefix:@"douyin://"])
    {
        [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
        return ;
    }

    NSURL *formatUrl = [NSURL URLWithString:[url stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    if ([[UIApplication sharedApplication] canOpenURL:formatUrl]) {
        [[UIApplication sharedApplication] openURL:formatUrl];
    } else{
        [self failWithCallbackID:command.callbackId withMessage:@"未安装抖音或其他错误"];
    }
    return ;
}





#pragma mark "CDVPlugin Overrides"

//- (void)handleOpenURL:(NSNotification *)notification
//{
//    NSURL* url = [notification object];
//
//    if ([url isKindOfClass:[NSURL class]] && [url.scheme isEqualToString:self.kwaisAppId])
//    {
//        [KSApi handleOpenURL:url];
//    }
//}

#pragma mark "Private methods"

#pragma mark NSSessionUrlDelegate
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
      didWriteData:(int64_t)bytesWritten
 totalBytesWritten:(int64_t)totalBytesWritten
totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite
{
    //下载进度
    CGFloat progress = totalBytesWritten / (double)totalBytesExpectedToWrite;
    dispatch_async(dispatch_get_main_queue(), ^{
        //进行UI操作  设置进度条
        
        NSLog(@"下载进度： %@",[NSString stringWithFormat:@"%.2f%%",progress*100]) ;
    });
}
    //下载完成 保存到本地相册
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
didFinishDownloadingToURL:(NSURL *)location
{
    //1.拿到cache文件夹的路径
    NSString *cache=[NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES)lastObject];
    //2,拿到cache文件夹和文件名
    NSString *file=[cache stringByAppendingPathComponent:downloadTask.response.suggestedFilename];
    
    [[NSFileManager defaultManager] moveItemAtURL:location toURL:[NSURL fileURLWithPath:file] error:nil];
    //3，保存视频到相册
//    if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(file)) {
//        //保存相册核心代码
//        UISaveVideoAtPathToSavedPhotosAlbum(file, self, nil, nil);
//    }
    
    __block NSString *createdAssetID =nil;//唯一标识，可以用视频资源获取
    
    [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
        createdAssetID = [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:[NSURL fileURLWithPath:file]].placeholderForCreatedAsset.localIdentifier;

            } completionHandler:^(BOOL success, NSError * _Nullable error) {
                if (success) {
                    NSLog(@"video createdAssetID Succsee  %@",createdAssetID);
                 
                    
                    DouyinOpenSDKShareRequest *req = [[DouyinOpenSDKShareRequest alloc] init];
                    req.mediaType = DouyinOpenSDKShareMediaTypeVideo;
                    req.localIdentifiers = @[createdAssetID];//NSArray
                    
                    req.hashtag  = [self.shareDic objectForKey:@"title"];
                
                    dispatch_async(dispatch_get_main_queue(), ^{
                      BOOL succes = [req sendShareRequestWithCompleteBlock:^(DouyinOpenSDKShareResponse * _Nonnull respond) {


                        if (respond.isSucceed) {

                        // Share Succeed
                            self.currentCallbackId = self.command.callbackId;
                            [self successWithCallbackID:self.currentCallbackId];


                        } else{
                            NSLog(@"share error code:%@",@(respond.shareState) );
                            
                            [self failWithCallbackID:self.command.callbackId withMessage:@"发送请求失败"];
                            
                            self.currentCallbackId = nil;

                        }

                        }];

                    });
                    
                } else {
                    NSLog(@"video createdAssetID Fail %@",createdAssetID);
                    [self failWithCallbackID:self.command.callbackId withMessage:@"视频保存出错了"];

                }

            }];
}


- (void )getVideoUrl:(NSString *)url{
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
       NSURLSession *session = [NSURLSession sessionWithConfiguration:config delegate:self delegateQueue:[NSOperationQueue mainQueue]];
       
        self.downloadTask = [session downloadTaskWithURL:[NSURL URLWithString:url]];
       [self.downloadTask resume];
    
}
-(void)getVideosIdsWithUrls:(NSArray *)urlArray {
    if (!urlArray.count) {
        return;
    }
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
        if (status == PHAuthorizationStatusNotDetermined) {
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                if(status == PHAuthorizationStatusAuthorized) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        // 用户点击 "OK"
                    });
                } else {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        // 用户点击 不允许访问
                    });
                }
            }];
        }
    
    
//    for (int i =0 ; i < urlArray.count; i++) {
        // 目前先支持1个视频
         [self getVideoUrl:urlArray[0]];
//    }
}

-(void)getImageIdsWithImageUrls:(NSArray *)urlArray  doneBlock:(void(^)(NSArray *urls))result{
    if (!urlArray.count) {
//        return @[];
        result(@[]);
        return;
    }
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
        if (status == PHAuthorizationStatusNotDetermined) {
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                if(status == PHAuthorizationStatusAuthorized) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        // 用户点击 "OK"
                    });
                } else {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        // 用户点击 不允许访问
                    });
                }
            }];
        }
    NSMutableArray *ids = [NSMutableArray array];
    __block int count = 0;
    for (int i =0 ; i < urlArray.count; i++) {
        UIImage *image = [self getUIImageFromURL:urlArray[i]];
        
        __block NSString *createdAssetID =nil;//唯一标识，可以用于图片资源获取
        
        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
            createdAssetID = [PHAssetChangeRequest creationRequestForAssetFromImage:image].placeholderForCreatedAsset.localIdentifier;
            
                } completionHandler:^(BOOL success, NSError * _Nullable error) {
                    if (success) {
                        [ids addObject:createdAssetID];
                        NSLog(@"createdAssetID Succsee %d: %@",i,createdAssetID);
                    } else {
                        NSLog(@"createdAssetID Fail %d: %@",i,createdAssetID);
                    }
                    ++count;

                    if (count == urlArray.count) {
                        result(ids);
                    }
                }];
    }
}

- (NSData *)getNSDataFromURL:(NSString *)url
{
    NSData *data = nil;

    if ([url hasPrefix:@"http://"] || [url hasPrefix:@"https://"])
    {
        data = [NSData dataWithContentsOfURL:[NSURL URLWithString:url]];
    }
    else if ([url hasPrefix:@"data:image"])
    {
        // a base 64 string
        NSURL *base64URL = [NSURL URLWithString:url];
        data = [NSData dataWithContentsOfURL:base64URL];
    }
    else if ([url rangeOfString:@"temp:"].length != 0)
    {
        url =  [NSTemporaryDirectory() stringByAppendingPathComponent:[url componentsSeparatedByString:@"temp:"][1]];
        data = [NSData dataWithContentsOfFile:url];
    }
    else
    {
        // local file
        url = [[NSBundle mainBundle] pathForResource:[url stringByDeletingPathExtension] ofType:[url pathExtension]];
        data = [NSData dataWithContentsOfFile:url];
    }

    return data;
}

- (UIImage *)getUIImageFromURL:(NSString *)url
{
    NSData *data = [self getNSDataFromURL:url];
    UIImage *image = [UIImage imageWithData:data];

    if (image.size.width > MAX_THUMBNAIL_SIZE || image.size.height > MAX_THUMBNAIL_SIZE)
    {
        CGFloat width = 0;
        CGFloat height = 0;

        // calculate size
        if (image.size.width > image.size.height)
        {
            width = MAX_THUMBNAIL_SIZE;
            height = width * image.size.height / image.size.width;
        }
        else
        {
            height = MAX_THUMBNAIL_SIZE;
            width = height * image.size.width / image.size.height;
        }

        // scale it
        UIGraphicsBeginImageContext(CGSizeMake(width, height));
        [image drawInRect:CGRectMake(0, 0, width, height)];
        UIImage *scaled = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();

        return scaled;
    }

    return image;
}

- (void)successWithCallbackID:(NSString *)callbackID
{
    [self successWithCallbackID:callbackID withMessage:@"OK"];
}

- (void)successWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

- (void)failWithCallbackID:(NSString *)callbackID withError:(NSError *)error
{
    [self failWithCallbackID:callbackID withMessage:[error localizedDescription]];
}

- (void)failWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}
    
@end

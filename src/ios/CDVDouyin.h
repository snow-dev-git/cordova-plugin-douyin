//
//  CDVDouyin.m
//  cordova-plugin-jasonhe-douyin
//
//  Created by 何助金 on 21/15/01.
//
//

#import <Cordova/CDV.h>
#import "DouyinOpenSDK.framework/Headers/DouyinOpenSDKShare.h"
#import "DouyinOpenSDK.framework/Headers/DouyinOpenSDKApplicationDelegate.h"

@interface CDVDouyin:CDVPlugin <NSURLSessionDelegate>

@property (nonatomic, strong) NSString *currentCallbackId;
@property (nonatomic, strong) NSString *douyinAppId;
//@property (nonatomic, strong) NSString *universalLink;
@property(nonatomic,strong) NSURLSessionDownloadTask *downloadTask;
@property(nonatomic,strong) CDVInvokedUrlCommand *command;
@property(nonatomic,strong) NSDictionary *shareDic;

- (void)isDouyinAppInstalled:(CDVInvokedUrlCommand *)command;
- (void)share:(CDVInvokedUrlCommand *)command;
- (void)sendAuthRequest:(CDVInvokedUrlCommand *)command;
- (void)jumpToDouyin:(CDVInvokedUrlCommand *)command;

@end



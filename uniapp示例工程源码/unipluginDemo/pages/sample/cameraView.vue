<template>
	<view class="content">
		<!-- 录制视频容器 -->
		<view class="cameraView" v-if="false">
			<!-- 单录 -->
			<view class="one" v-if="cameraType == 'one'">

			</view>
			<!-- 照片 -->
			<view class="photo" v-if="cameraType == 'photo'"></view>
			<!-- 双录 -->
			<view class="two" v-if="cameraType == 'two'"></view>
		</view>
		<view class="operation_container" v-if="false">
			<!-- 切换功能 -->
			<view class="top">
				<button @click="cameraType = 'one'">录像</button>
				<button @click="cameraType = 'photo'">拍照</button>
				<button @click="cameraType = 'two'">双景</button>
			</view>
			<view class="bot">
				<!-- 录制按钮 -->
				<view class="startBut">
					<view class="butBg" @click="recording()">
						{{cameraType = 'photo' ? "拍照" : "录制"}}
					</view>
				</view>
				<!-- 切换前后摄像头 -->
				<view class="switch" v-if="cameraType != 'two'">
					<view class="switchIn" @click="cameraSwitch()">切换</view>
				</view>
			</view>
		</view>
		<button type="primary" @click="testAsyncFunc">testAsyncFunc</button>
		<button type="primary" @click="testSyncFunc">testSyncFunc</button>
		<button @click="initCamera()">点击跳转到原生页面预览</button>
	</view>
</template>

<script>
	// 获取 module 
	const cameraModule = uni.requireNativePlugin('CameraModule');
	const modal = uni.requireNativePlugin('modal');
	export default {
		onLoad() {

		},
		methods: {
			// 初始化界面 默认单录 后置摄像头
			initCamera() {
				uni.showToast({
					title: "初始化",
					icon: 'none'
				})
				cameraModule.gotoCameraPage()
			},
			testAsyncFunc() {
				// 调用异步方法
				cameraModule.testAsyncFunc({
						'name': 'unimp',
						'age': 1
					},
					(ret) => {
						modal.toast({
							message: ret,
							duration: 1.5
						});
					})
			},
			testSyncFunc() {
				// 调用同步方法
				var ret = cameraModule.testSyncFunc({
					'name': 'unimp',
					'age': 1
				})
				modal.toast({
					message: ret,
					duration: 1.5
				});
			},
			recording() {

			},
			cameraSwitch() {

			}
		}
	}
</script>
<style lang="scss">
	page {
		height: 100%;
		width: 100%;
	}

	.content {
		width: 100%;
		height: 100%;
		position: relative;
		overflow: hidden;
		background-color: #000;
		// background-color: rgba(red, green, blue, alpha);

		.cameraView {
			width: 100%;
			height: 100%;
		}

		.operation_container {
			position: absolute;
			width: 100%;
			bottom: 0;

			.top {
				width: 100%;
				padding-left: 50vw;
				display: flex;
				box-sizing: border-box;

				>view {
					flex: 1
				}
			}

			.bot {
				width: 100%;
				height: 150rpx;
				margin-top: 50rpx;
				margin-bottom: 150rpx;
				position: relative;
				display: flex;
				align-items: center;
				justify-content: center;


				.startBut {
					width: 150rpx;
					height: 150rpx;
					border-radius: 150rpx;
					font-size: 14rpx;
					overflow: hidden;
					background-color: rgba(139, 138, 138, 0.49);
					display: flex;
					align-items: center;
					justify-content: center;

					position: absolute;
					bottom: 0px;
					left: 0px;
					right: 0px;
					margin: auto;

					.butBg {
						background-color: #ffffff;
						width: 80%;
						height: 80%;
						font-size: 16px;
						overflow: hidden;
						border-radius: 80%;
						display: flex;
						align-items: center;
						justify-content: center;
					}
				}

				.switch {
					width: 100rpx;
					height: 100rpx;
					border-radius: 100rpx;
					overflow: hidden;
					background-color: rgba(139, 138, 138, 0.49);
					display: flex;
					align-items: center;
					justify-content: center;

					position: absolute;
					top: 0px;
					bottom: 0px;
					right: 50rpx;
					margin: auto;

					.switchIn {
						width: 80%;
						height: 80%;
						border-radius: 80%;
						background-color: #ffffff;
						font-size: 14px;
						display: flex;
						align-items: center;
						justify-content: center;
					}
				}

			}

		}
	}
</style>
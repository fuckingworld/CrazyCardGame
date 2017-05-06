package com.sdk.dianjin.pay.util;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.bodong.dpaysdk.DPayConfig;
import com.bodong.dpaysdk.DPayManager;
import com.bodong.dpaysdk.entity.DPayRechargeOrder;
import com.bodong.dpaysdk.listener.DPayLoginListener;
import com.bodong.dpaysdk.listener.DPayLogoutListener;
import com.bodong.dpaysdk.listener.DPayRechargeListener;
import com.bodong.dpaysdk.listener.DPaySDKExitListener;
import com.lordcard.common.util.DialogUtils;
import com.lordcard.common.util.JsonHelper;
import com.lordcard.constant.Database;
import com.lordcard.entity.JsonResult;
import com.lordcard.net.http.HttpRequest;
import com.sdk.dianjin.pay.DJConstant;
import com.sdk.dianjin.pay.DpayOrder;

public class DJPayUtil {
	private static String orderid;

	// 充值监听
	static DPayRechargeListener mDPayRechargeListener = new DPayRechargeListener() {

		/**
		 * @Title: onRechargeSubmitted
		 * @Description: 订单已经提交到平台服务器处理，若开发者使用代币模式，可在此将订单信息保存到游戏服务端，
		 *               后续平台服务器通知游戏服务器时，游戏服务器可根据此单据进行比对处理。
		 * 
		 * @param rechargeOrder
		 *            : 订单详情
		 */
		@Override
		public void onRechargeSubmitted(DPayRechargeOrder order) {
			Log.i("TAG", "订单已经提交平台服务器：" + order.rechargeId + " 开发者传入订单号：" + order.uRechargeId + " extra：" + order.extra + " 用户充值金额（人民币）："
					+ order.money + " 用户充值金额（游戏币）：" + order.amount);
		}

		/**
		 * @Title: onRechargeFinished
		 * @Description: 用户完成充值流程，开发者可在此根据订单的状态，进行自身逻辑处理。推荐处理方式为：
		 *               1、若订单为到帐（用户充值成功），开发者处理游戏客户端后续逻辑，例如：继续购买操作。
		 *               2、若订单为失败（用户充值失败），开发者可提示用户充值失败等。
		 *               3、若订单为未到帐（短信/充值卡等充值方式会有一定时间的延迟），推荐的处理方式为：
		 *               游戏客户端查询游戏服务器此订单状态 若订单状态还是未到帐，则弹出提示窗口，提醒用户充值还未到帐，是否重新查询
		 * 
		 *               订单状态可参见：
		 * @see DPayConfig.RechargeStatus
		 * @param rechargeOrder
		 *            : 订单详情，若订单还未提交到平台服务器（用户还未进行充值），则为空
		 */
		@Override
		public void onRechargeFinished(DPayRechargeOrder order) {
			if (order != null) {
				// 充值订单状态
				int rechargeResult = order.status;
				// 平台充值订单号
				String rechargeId = order.rechargeId;
				// 开发者传入订单号
				String uRechargeId = order.uRechargeId;
				// 开发者传入附带值
				String extra = order.extra;
				// 用户充值人民币，以元为单位
				float money = order.money;
				// 用户充值游戏币个数
				int amount = order.amount;
				if (rechargeResult == DPayConfig.RechargeStatus.TRANSFERED) {
					// 处理充值成功逻辑
					// progressDialog.dismiss();
					if(Database.chargingProcessDia != null && Database.chargingProcessDia.isShowing())
					{
						Database.chargingProcessDia.dismiss();
					}
					DialogUtils.toastTip("用户充值成功");
				} else if (rechargeResult == DPayConfig.RechargeStatus.UNTRANSFERED) {
					// 处理充值未到帐，开发者可利用充值订单到游戏服务器查询此订单状态。
					// progressDialog.dismiss();
					DialogUtils.toastTip("处理充值未到帐");
				} else if (rechargeResult == DPayConfig.RechargeStatus.TRANSFER_FAILED) {
					// 处理充值失败
					// progressDialog.dismiss();
					DialogUtils.toastTip("用户充值失败");
				}
				Log.i("TAG", "充值订单号：" + rechargeId + " 开发者传入订单号：" + uRechargeId + " extra：" + extra + " 充值订单状态：" + rechargeResult + " 用户充值金额（人民币）："
						+ money + " 用户充值金额（游戏币）：" + amount);

			} else {
				// 用户取消，订单还未提交到平台服务器处理
				Log.i("TAG", "感谢使用点金游戏平台充值，欢迎下次回来～");
			}
		}
	};

	public static void djInit() {

		// 初始化sdk
		DPayManager.init(Database.currentActivity.getApplicationContext());
		DPayManager.setSDKExitListener(new DPaySDKExitListener() {
			@Override
			public void onExit() {
				Log.i("TAG", "客户端知道你退出了哦～");
			}
		});
		// 设置充值监听
		DPayManager.setRechargeListener(mDPayRechargeListener);
		// 设置登录监听
		DPayManager.setLoginListener(new DPayLoginListener() {
			@Override
			public void onLogin() {
				if (DPayManager.isUserLoggedIn()) {
					Log.i("TAG", "客户端知道你登录了哦～");
					Log.i("TAG", "登录信息为：" + DPayManager.getUserId() + " -- " + DPayManager.getSessionId());
				}
			}
		});
		// 设置注销监听
		DPayManager.setLogoutListener(new DPayLogoutListener() {
			@Override
			public void onLogout() {
				Log.i("TAG", "客户端知道你注销了哦～");
			}
		});
	}

	public static void DJDestroy() {
		// 移除平台监听
		DPayManager.setRechargeListener(null);
		DPayManager.setSDKExitListener(null);
		DPayManager.setLoginListener(null);
		DPayManager.setLogoutListener(null);
	}

	/**
	 * 点金支付
	 * 
	 */
	public static void goDJPay(final String money, final Context context) {

		new Thread() {
			public void run() {
				// 先提交充值订单
				Map<String, String> paramMap = new HashMap<String, String>();

				paramMap.put("money", money);
				paramMap.put("loginToken", Database.USER.getLoginToken());
				final int moneyPay = Integer.parseInt(money);// 上传的是金豆，
				// 后台生成订单
				String resultJson = HttpRequest.addPayOrder(DJConstant.DJPAY_URL, paramMap);
				JsonResult result = JsonHelper.fromJson(resultJson, JsonResult.class);
				if (JsonResult.SUCCESS.equals(result.getMethodCode())) {
					final DpayOrder payOrder = JsonHelper.fromJson(result.getMethodMessage(), DpayOrder.class);
					orderid = payOrder.getOrderNo();
					Database.currentActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {

							DPayManager.startRechargeActivity(context, orderid, "", moneyPay);
						}
					});

				} else {
					DialogUtils.mesTip(result.getMethodMessage(), true);
				}
			};
		}.start();
	}

}

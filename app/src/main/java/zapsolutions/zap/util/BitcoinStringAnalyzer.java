package zapsolutions.zap.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.lightningnetwork.lnd.lnrpc.PayReq;

import java.net.URL;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import zapsolutions.zap.R;
import zapsolutions.zap.connection.RemoteConfiguration;
import zapsolutions.zap.connection.manageWalletConfigs.WalletConfigsManager;
import zapsolutions.zap.lightning.LightningNodeUri;
import zapsolutions.zap.lightning.LightningParser;
import zapsolutions.zap.lnurl.channel.LnUrlChannelResponse;
import zapsolutions.zap.lnurl.channel.LnUrlHostedChannelResponse;
import zapsolutions.zap.lnurl.pay.LnUrlPayResponse;
import zapsolutions.zap.lnurl.withdraw.LnUrlWithdrawResponse;

public class BitcoinStringAnalyzer {

    public static void analyze(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        checkIfLnUrl(ctx, compositeDisposable, inputString, listener);
    }

    private static void checkIfLnUrl(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        LnUrlUtil.readLnUrl(ctx, inputString, new LnUrlUtil.OnLnUrlReadListener() {
            @Override
            public void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse) {
                listener.onValidLnUrlWithdraw(withdrawResponse);
            }

            @Override
            public void onValidLnUrlPay(LnUrlPayResponse payResponse) {
                listener.onValidLnUrlPay(payResponse);
            }

            @Override
            public void onValidLnUrlChannel(LnUrlChannelResponse channelResponse) {
                listener.onValidLnUrlChannel(channelResponse);
            }

            @Override
            public void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse) {
                listener.onValidLnUrlHostedChannel(hostedChannelResponse);
            }

            @Override
            public void onValidLnUrlAuth(URL url) {
                listener.onValidLnUrlAuth(url);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoLnUrlData() {
                checkIfRemoteConnection(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfRemoteConnection(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        RemoteConnectUtil.decodeConnectionString(ctx, inputString, new RemoteConnectUtil.OnRemoteConnectDecodedListener() {
            @Override
            public void onValidLndConnectString(RemoteConfiguration remoteConfiguration) {
                listener.onValidLndConnectString(remoteConfiguration);
            }

            @Override
            public void onValidBTCPayConnectData(RemoteConfiguration remoteConfiguration) {
                listener.onValidBTCPayConnectData(remoteConfiguration);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoConnectData() {
                checkIfNodeUri(ctx, compositeDisposable, inputString, listener);
            }
        });
    }

    private static void checkIfNodeUri(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        LightningNodeUri nodeUri = LightningParser.parseNodeUri(inputString);

        if (nodeUri != null) {
            listener.onValidNodeUri(nodeUri);

        } else {
            if (WalletConfigsManager.getInstance().hasAnyConfigs()) {
                checkIfLnOrBitcoinInvoice(ctx, compositeDisposable, inputString, listener);
            } else {
                listener.onError(ctx.getString(R.string.demo_setupWalletFirst), RefConstants.ERROR_DURATION_SHORT);
            }
        }
    }

    private static void checkIfLnOrBitcoinInvoice(Context ctx, CompositeDisposable compositeDisposable, @NonNull String inputString, OnDataDecodedListener listener) {
        InvoiceUtil.readInvoice(ctx, compositeDisposable, inputString, new InvoiceUtil.OnReadInvoiceCompletedListener() {
            @Override
            public void onValidLightningInvoice(PayReq paymentRequest, String invoice) {
                listener.onValidLightningInvoice(paymentRequest, invoice);
            }

            @Override
            public void onValidBitcoinInvoice(String address, long amount, String message) {
                listener.onValidBitcoinInvoice(address, amount, message);
            }

            @Override
            public void onError(String error, int duration) {
                listener.onError(error, duration);
            }

            @Override
            public void onNoInvoiceData() {
                // No Invoice or Address either, we have unrecognizable data
                listener.onError(ctx.getString(R.string.string_analyzer_unrecognized_data), RefConstants.ERROR_DURATION_SHORT);
            }
        });
    }


    public interface OnDataDecodedListener {
        void onValidLightningInvoice(PayReq paymentRequest, String invoice);

        void onValidBitcoinInvoice(String address, long amount, String message);

        void onValidLnUrlWithdraw(LnUrlWithdrawResponse withdrawResponse);

        void onValidLnUrlChannel(LnUrlChannelResponse channelResponse);

        void onValidLnUrlHostedChannel(LnUrlHostedChannelResponse hostedChannelResponse);

        void onValidLnUrlPay(LnUrlPayResponse payResponse);

        void onValidLnUrlAuth(URL url);

        void onValidLndConnectString(RemoteConfiguration remoteConfiguration);

        void onValidBTCPayConnectData(RemoteConfiguration remoteConfiguration);

        void onValidNodeUri(LightningNodeUri nodeUri);

        void onError(String error, int duration);
    }
}

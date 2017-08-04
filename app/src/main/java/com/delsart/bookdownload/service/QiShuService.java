package com.delsart.bookdownload.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.delsart.bookdownload.MsgType;
import com.delsart.bookdownload.Url;
import com.delsart.bookdownload.bean.DownloadBean;
import com.delsart.bookdownload.bean.NovelBean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class QiShuService extends BaseService {
    private final Handler mHandler;
    private int mPage;
    private String mBaseUrl;
    private CountDownLatch latch;
    private ArrayList<NovelBean> list = new ArrayList<>();
    private static String TAG = "test";

    public QiShuService(Handler handler, String keywords) {
        super(handler, keywords);
        this.mHandler = handler;
        mPage = 0;
        mBaseUrl = Url.QISHU + toUtf8(keywords) + "&p=";
    }

    @Override
    public void get() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<NovelBean> list2 = new ArrayList<>();
                    list2.addAll(list);
                    list.clear();
                    Elements select = Jsoup.connect(mBaseUrl + mPage)
                            .timeout(10000)
                            //.ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.PC_AGENT)
                            .get()
                            .select("div#results").select("h3.c-title").select("a");
                    latch = new CountDownLatch(select.size());
                    for (int i = 0; i < select.size(); i++) {
                        runInSameTime(select.get(i));
                    }
                    latch.await();
                    if (list2.size() > 0) {
                        if (list2.size() > 0 && list2.get(0).getShowText().equals(list.get(0).getShowText()))
                            list.clear();
                    }
                    mPage++;
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.SUCCESS;
                    msg.obj = list;
                    mHandler.sendMessage(msg);
                } catch (SocketException e) {
                    e.printStackTrace();
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.ERROR;
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.ERROR;
                    mHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void runInSameTime(final Element element) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = element.attr("abs:href");
                if (!url.contains(".html")) {
                    latch.countDown();
                    return;
                }
                Document document = null;
                try {
                    document = Jsoup.connect(url)
                            // .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .get();
                    Elements a = document.select("div.detail");
                    if (a.select("li").size() < 7) {
                        Log.d(TAG, "run: " + url);
                        latch.countDown();
                        return;
                    }
                    String name = a.select("h1").text();
                    String time = a.select("li.small").get(4).text();
                    String info = document.select("body > div:nth-child(4) > div.show > div:nth-child(2) > div.showInfo").text();
                    String category = a.select("li.small").get(3).text();
                    String status = a.select("li.small").get(5).text();
                    String author = a.select("li.small").get(6).text();
                    String words = a.select("li.small").get(2).text();
                    String pic = a.select("img").attr("abs:src");
                    NovelBean no = new NovelBean(name, time, info, category, status, author, words, pic, url);
                    list.add(no);
                    latch.countDown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    public ArrayList<DownloadBean> getDownloadurls(final String url) throws InterruptedException {
        latch = new CountDownLatch(1);
        final ArrayList<DownloadBean> urls = new ArrayList<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Document document = null;
                try {
                    document = Jsoup.connect(url)
                            .timeout(10000)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String u1 = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(1) > a").attr("abs:href");
                String u1n = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(1) > a").text();
                String u2 = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(2) > a").attr("abs:href");
                String u2n = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(2) > a").text();
                String u3 = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(3) > a").attr("abs:href");
                String u3n = document.select("body > div:nth-child(4) > div.show > div:nth-child(4) > div.showDown > ul > li:nth-child(3) > a").text();
                urls.add(new DownloadBean(u1n, u1));
                urls.add(new DownloadBean(u2n, u2));
                urls.add(new DownloadBean(u3n, u3));
                latch.countDown();
            }
        }).start();
        latch.await();
        return urls;
    }

}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static android.R.id.list;

public class AiXiaService extends BaseService {
    private final Handler mHandler;
    private int mPage;
    private String mBaseUrl;
    private CountDownLatch latch;
    private ArrayList<NovelBean> list = new ArrayList<>();
    private static String TAG = "test";

    public AiXiaService(Handler handler, String keywords) {
        super(handler, keywords);
        this.mHandler = handler;
        mPage = 1;
        mBaseUrl = Url.AIXIA + keywords + "&page=";
    }

    @Override
    public void get() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    list.clear();
                    Elements select = Jsoup.connect(mBaseUrl + mPage)
                            .timeout(10000)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get()
                            .select("body > div.list > li > a");
                    latch = new CountDownLatch(select.size());
                    for (int i = 0; i < select.size(); i++) {
                        runInSameTime(select.get(i));
                    }
                    latch.await();
                    mPage++;
                    Message msg = mHandler.obtainMessage();
                    msg.what = MsgType.SUCCESS;
                    msg.obj = list;
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
                Document document = null;
                try {
                    document = Jsoup.connect(url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .userAgent(Url.MOBBILE_AGENT)
                            .get();

                String name = document.select("body > div:nth-child(2) > h1").text();
                String time = document.select("body > div:nth-child(3) > li:nth-child(6)").text();
                String info = document.select("body > div.intro > li").text();
                String category = document.select("body > div:nth-child(3) > li:nth-child(2)").text();
                String status = document.select("body > div:nth-child(3) > li:nth-child(5)").text();
                String author = document.select("body > div:nth-child(3) > li:nth-child(1)").text();
                String words = document.select("body > div:nth-child(3) > li:nth-child(3)").text();
                String pic = "";

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
                String u1 = document.select("body > div:nth-child(5) > li:nth-child(2) > a").attr("abs:href");
                String u1n = document.select("body > div:nth-child(5) > li:nth-child(2) > a").text();
                String u2 = document.select("body > div:nth-child(5) > li:nth-child(3) > a").attr("abs:href");
                String u2n = document.select("body > div:nth-child(5) > li:nth-child(3) > a").text();
                urls.add(new DownloadBean(u1n, u1));
                urls.add(new DownloadBean(u2n, u2));
                latch.countDown();
            }
        }).start();
        latch.await();
        return urls;
    }

}

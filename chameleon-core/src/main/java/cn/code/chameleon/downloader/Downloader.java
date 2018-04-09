package cn.code.chameleon.downloader;

import cn.code.chameleon.carrier.Page;
import cn.code.chameleon.carrier.Request;

/**
 * @author liumingyu
 * @create 2018-04-08 下午1:15
 */
public interface Downloader {

    /**
     * 下载页面
     *
     * @param request
     * @return
     */
    Page download(Request request);

    /**
     * 设置线程数
     *
     * @param threadNum
     */
    void setThread(int threadNum);
}

package com.hotbitmapgg.ohmybilibili.module.home.bangumi;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hotbitmapgg.ohmybilibili.BilibiliApp;
import com.hotbitmapgg.ohmybilibili.R;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiBannerSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiItemSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiNewSerialSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiRecommendSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiSeasonNewSection;
import com.hotbitmapgg.ohmybilibili.base.RxLazyFragment;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.BangumiRecommend;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.HomeBangumiRecommend;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.NewBangumiSerial;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.SeasonNewBangumi;
import com.hotbitmapgg.ohmybilibili.utils.SnackbarUtil;
import com.hotbitmapgg.ohmybilibili.widget.CustomEmptyView;
import com.hotbitmapgg.ohmybilibili.widget.banner.BannerEntity;
import com.hotbitmapgg.ohmybilibili.widget.sectioned.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.rx_cache.Reply;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by hcc on 16/8/4 21:18
 * 100332338@qq.com
 * <p/>
 * 首页番剧界面
 */
public class HomeBangumiFragment extends RxLazyFragment
{

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.recycle)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_layout)
    CustomEmptyView mCustomEmptyView;

    private boolean mIsRefreshing = false;

    private boolean mIsCacheRefresh = false;

    private List<BannerEntity> bannerList = new ArrayList<>();

    private List<BangumiRecommend.ResultBean> bangumiRecommends = new ArrayList<>();

    private List<HomeBangumiRecommend.ResultBean.BannersBean> banners = new ArrayList<>();

    private List<NewBangumiSerial.ListBean> newBangumiSerials = new ArrayList<>();

    private List<SeasonNewBangumi.ListBean> seasonNewBangumis = new ArrayList<>();

    private SectionedRecyclerViewAdapter mSectionedRecyclerViewAdapter;

    public static HomeBangumiFragment newInstance()
    {

        return new HomeBangumiFragment();
    }

    @Override
    public int getLayoutResId()
    {

        return R.layout.fragment_home_bangumi;
    }

    @Override
    public void finishCreateView(Bundle state)
    {

        initRefreshLayout();
        initRecyclerView();
    }

    @Override
    protected void initRecyclerView()
    {

        mSectionedRecyclerViewAdapter = new SectionedRecyclerViewAdapter();
        GridLayoutManager mGridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
        {

            @Override
            public int getSpanSize(int position)
            {

                switch (mSectionedRecyclerViewAdapter.getSectionItemViewType(position))
                {
                    case SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER:
                        return 3;

                    default:
                        return 1;
                }
            }
        });

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mRecyclerView.setAdapter(mSectionedRecyclerViewAdapter);
        setRecycleNoScroll();
    }

    @Override
    protected void initRefreshLayout()
    {

        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.post(() -> {

            mSwipeRefreshLayout.setRefreshing(true);
            mIsRefreshing = true;
            loadData();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {

            clearData();
            loadData();
        });
    }


    private void clearData()
    {

        mIsRefreshing = true;
        mIsCacheRefresh = true;
        banners.clear();
        bangumiRecommends.clear();
        newBangumiSerials.clear();
        seasonNewBangumis.clear();
        mSectionedRecyclerViewAdapter.removeAllSections();
    }


    @Override
    protected void loadData()
    {

        BilibiliApp.getInstance().getRepository()
                .getHomeBangumiRecommended(mIsCacheRefresh)
                .compose(bindToLifecycle())
                .map(homeBangumiRecommendReply -> homeBangumiRecommendReply.getData().getResult().getBanners())
                .flatMap(new Func1<List<HomeBangumiRecommend.ResultBean.BannersBean>,Observable<Reply<SeasonNewBangumi>>>()
                {

                    @Override
                    public Observable<Reply<SeasonNewBangumi>> call(List<HomeBangumiRecommend.ResultBean.BannersBean> bannersBeans)
                    {

                        banners.addAll(bannersBeans);
                        return BilibiliApp.getInstance().getRepository().getSeasonNewBangumiList(mIsCacheRefresh);
                    }
                })
                .compose(bindToLifecycle())
                .map(seasonNewBangumiReply -> seasonNewBangumiReply.getData().getList())
                .flatMap(new Func1<List<SeasonNewBangumi.ListBean>,Observable<Reply<NewBangumiSerial>>>()
                {

                    @Override
                    public Observable<Reply<NewBangumiSerial>> call(List<SeasonNewBangumi.ListBean> listBeans)
                    {

                        seasonNewBangumis.addAll(listBeans);
                        return BilibiliApp.getInstance().getRepository().getNewBangumiSerialList(mIsCacheRefresh);
                    }
                })
                .compose(bindToLifecycle())
                .map(newBangumiSerialReply -> newBangumiSerialReply.getData().getList())
                .flatMap(new Func1<List<NewBangumiSerial.ListBean>,Observable<Reply<BangumiRecommend>>>()
                {

                    @Override
                    public Observable<Reply<BangumiRecommend>> call(List<NewBangumiSerial.ListBean> listBeans)
                    {

                        newBangumiSerials.addAll(listBeans);
                        return BilibiliApp.getInstance().getRepository().getBangumiRecommended(mIsCacheRefresh);
                    }
                })
                .compose(bindToLifecycle())
                .map(bangumiRecommendReply -> bangumiRecommendReply.getData().getResult())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bangumiDetailsRecommend -> {

                    bangumiRecommends.addAll(bangumiDetailsRecommend);
                    finishTask();
                }, throwable -> {
                    initEmptyView();
                });
    }

    @Override
    protected void finishTask()
    {

        mSwipeRefreshLayout.setRefreshing(false);
        mIsRefreshing = false;
        hideEmptyView();

        BannerEntity banner;
        for (int i = 0, size = banners.size(); i < size; i++)
        {
            banner = new BannerEntity();
            HomeBangumiRecommend.ResultBean.BannersBean bannersBean = banners.get(i);
            banner.img = bannersBean.getImg();
            banner.link = bannersBean.getLink();
            bannerList.add(banner);
        }
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiBannerSection(bannerList));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiItemSection(getActivity()));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiNewSerialSection(getActivity(), newBangumiSerials));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiSeasonNewSection(getActivity(), seasonNewBangumis));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiRecommendSection(getActivity(), bangumiRecommends));
        mSectionedRecyclerViewAdapter.notifyDataSetChanged();
    }

    public void initEmptyView()
    {

        mSwipeRefreshLayout.setRefreshing(false);
        mCustomEmptyView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mCustomEmptyView.setEmptyImage(R.drawable.img_tips_error_load_error);
        mCustomEmptyView.setEmptyText("加载失败~(≧▽≦)~啦啦啦.");
        SnackbarUtil.showMessage(mRecyclerView, "数据加载失败,请重新加载或者检查网络是否链接");
    }

    public void hideEmptyView()
    {

        mCustomEmptyView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setRecycleNoScroll()
    {

        mRecyclerView.setOnTouchListener((v, event) -> mIsRefreshing);
    }
}

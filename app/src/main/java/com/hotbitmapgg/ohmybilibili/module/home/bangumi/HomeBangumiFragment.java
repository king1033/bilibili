package com.hotbitmapgg.ohmybilibili.module.home.bangumi;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hotbitmapgg.ohmybilibili.R;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiBannerSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiItemSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiNewSerialSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiRecommendSection;
import com.hotbitmapgg.ohmybilibili.adapter.section.HomeBangumiSeasonNewSection;
import com.hotbitmapgg.ohmybilibili.base.RxLazyFragment;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.BangumiRecommend;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.NewBangumiSerial;
import com.hotbitmapgg.ohmybilibili.entity.bangumi.SeasonNewBangumi;
import com.hotbitmapgg.ohmybilibili.network.RetrofitHelper;
import com.hotbitmapgg.ohmybilibili.utils.LogUtil;
import com.hotbitmapgg.ohmybilibili.utils.SnackbarUtil;
import com.hotbitmapgg.ohmybilibili.widget.CustomEmptyView;
import com.hotbitmapgg.ohmybilibili.widget.banner.BannerEntity;
import com.hotbitmapgg.ohmybilibili.widget.sectioned.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
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

    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Bind(R.id.recycle)
    RecyclerView mRecyclerView;

    @Bind(R.id.empty_layout)
    CustomEmptyView mCustomEmptyView;

    private boolean mIsRefreshing = false;

    private List<BannerEntity> bannerList = new ArrayList<>();

    private List<BangumiRecommend.RecommendsBean> recommends = new ArrayList<>();

    private List<BangumiRecommend.BannersBean> banners = new ArrayList<>();

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

        isPrepared = true;
        lazyLoad();
    }

    @Override
    protected void lazyLoad()
    {

        if (!isPrepared || !isVisible)

            return;
        showProgressBar();
        initRecyclerView();
        isPrepared = false;
    }

    private void initRecyclerView()
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

    private void showProgressBar()
    {

        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.post(() -> {

            mSwipeRefreshLayout.setRefreshing(true);
            mIsRefreshing = true;
            getBangumiRecommends();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {

            clearData();
            getBangumiRecommends();
        });
    }

    private void clearData()
    {

        mIsRefreshing = true;
        banners.clear();
        recommends.clear();
        newBangumiSerials.clear();
        seasonNewBangumis.clear();
        mSectionedRecyclerViewAdapter.removeAllSections();
    }


    /**
     * 获取番剧推荐数据
     * 包含Banner和番剧推荐内容
     * 获取二次元新番
     */
    private void getBangumiRecommends()
    {


        RetrofitHelper.getBnagumiRecommendApi()
                .getBangumiRecommended()
                .compose(this.bindToLifecycle())
                .flatMap(new Func1<BangumiRecommend,Observable<SeasonNewBangumi>>()
                {

                    @Override
                    public Observable<SeasonNewBangumi> call(BangumiRecommend bangumiRecommend)
                    {

                        banners.addAll(bangumiRecommend.getBanners());
                        recommends.addAll(bangumiRecommend.getRecommends());
                        return RetrofitHelper.getSeasonNewBangumiApi()
                                .getSeasonNewBangumiList();
                    }
                })
                .compose(this.bindToLifecycle())
                .flatMap(new Func1<SeasonNewBangumi,Observable<NewBangumiSerial>>()
                {

                    @Override
                    public Observable<NewBangumiSerial> call(SeasonNewBangumi seasonNewBangumi)
                    {

                        seasonNewBangumis.addAll(seasonNewBangumi.getList());
                        return RetrofitHelper.getNewBangumiSerial()
                                .getNewBangumiSerialList();
                    }
                })
                .compose(this.bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(newBangumiSerial -> {

                    newBangumiSerials.addAll(newBangumiSerial.getList());
                    finishTask();
                }, throwable -> {

                    LogUtil.all(throwable.getMessage());
                    initEmptyView();
                });
    }

    private void finishTask()
    {

        mSwipeRefreshLayout.setRefreshing(false);
        mIsRefreshing = false;
        hideEmptyView();

        BannerEntity banner;
        for (int i = 0, size = banners.size(); i < size; i++)
        {
            banner = new BannerEntity();
            BangumiRecommend.BannersBean bannersBean = banners.get(i);
            banner.img = bannersBean.getImg();
            banner.link = bannersBean.getLink();
            bannerList.add(banner);
        }
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiBannerSection(bannerList));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiItemSection(getActivity()));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiNewSerialSection(getActivity(), newBangumiSerials));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiSeasonNewSection(getActivity(), seasonNewBangumis));
        mSectionedRecyclerViewAdapter.addSection(new HomeBangumiRecommendSection(getActivity(), recommends));
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
        mCustomEmptyView.reload(this::showProgressBar);
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

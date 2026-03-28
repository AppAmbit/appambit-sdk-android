package com.appambit.javaapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appambit.javaapp.models.Post;
import com.appambit.sdk.Cms;
import com.appambit.sdk.services.interfaces.ICmsQuery;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CmsFragment extends Fragment {

    private CmsAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_cms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CmsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupFilters(view);
        loadPosts(Cms.content("blog_posts", Post.class));
    }

    private void setupFilters(View view) {
        view.findViewById(R.id.btn_filter_all).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class)));

        view.findViewById(R.id.btn_filter_tech).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).equals("category", "technology")));

        view.findViewById(R.id.btn_filter_not_equals).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).notEquals("category", "test")));

        view.findViewById(R.id.btn_filter_search).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).search("test")));

        view.findViewById(R.id.btn_filter_contains).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).contains("title", "st")));

        view.findViewById(R.id.btn_filter_starts_with).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).startsWith("body", "orem")));

        view.findViewById(R.id.btn_filter_in).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).inList("category", List.of("science", "technology"))));

        view.findViewById(R.id.btn_filter_not_in).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).notInList("category", List.of("technology", "test"))));

        view.findViewById(R.id.btn_filter_id).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).greaterThan("likes", 1000)));

        view.findViewById(R.id.btn_filter_gte).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).greaterThanOrEqual("rating", 4.3)));

        view.findViewById(R.id.btn_filter_less).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).lessThan("reading_time", 15)));

        view.findViewById(R.id.btn_filter_lte).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).lessThanOrEqual("reading_time", 15)));

        view.findViewById(R.id.btn_filter_sort_asc).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).orderByAscending("title")));

        view.findViewById(R.id.btn_filter_sort).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).orderByDescending("title")));

        view.findViewById(R.id.btn_filter_limit).setOnClickListener(v ->
            loadPosts(Cms.content("blog_posts", Post.class).getPage(1).getPerPage(2)));
    }

    private void loadPosts(ICmsQuery<Post> query) {
        executor.execute(() -> {
            try {
                query.getList()
                     .then(posts -> {
                        if (posts != null && !posts.isEmpty()) {
                        adapter.updatePosts(posts);
                        }
                     });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private class CmsAdapter extends RecyclerView.Adapter<CmsAdapter.CmsViewHolder> {
        private final List<Post> posts;

        public CmsAdapter(List<Post> posts) {
            this.posts = posts;
        }

        public void updatePosts(List<Post> newPosts) {
            this.posts.clear();
            this.posts.addAll(newPosts);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cms_post, parent, false);
            return new CmsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CmsViewHolder holder, int position) {
            Post post = posts.get(position);
            holder.txtTitle.setText(post.title);
            holder.txtBody.setText(post.body);
            holder.txtAuthorCategory.setText(post.author + " in " + post.category);
            
            holder.txtLikes.setText("❤️ " + post.likes);
            holder.txtRating.setText("⭐ " + post.rating + "/5");
            holder.txtReadingTime.setText("📖 " + post.readingTime + " min");
            holder.txtPublishedAt.setText(post.publishedAt != null ? post.publishedAt : "");

            if (post.featuredImage != null && !post.featuredImage.isEmpty()) {
                holder.imgFeatured.setVisibility(View.VISIBLE);
                loadImage(post.featuredImage, holder.imgFeatured);
            } else {
                holder.imgFeatured.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        private void loadImage(String url, ImageView imageView) {
            executor.execute(() -> {
                try {
                    InputStream in = new URL(url).openStream();
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) {
                        handler.post(() -> {
                            imageView.setImageBitmap(bmp);
                            imageView.setVisibility(View.VISIBLE);
                        });
                    } else {
                        handler.post(() -> imageView.setVisibility(View.GONE));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> imageView.setVisibility(View.GONE));
                }
            });
        }

        class CmsViewHolder extends RecyclerView.ViewHolder {
            ImageView imgFeatured;
            TextView txtTitle, txtBody, txtAuthorCategory;
            TextView txtLikes, txtRating, txtReadingTime, txtPublishedAt;

            public CmsViewHolder(@NonNull View itemView) {
                super(itemView);
                imgFeatured = itemView.findViewById(R.id.img_featured);
                txtTitle = itemView.findViewById(R.id.txt_title);
                txtBody = itemView.findViewById(R.id.txt_body);
                txtAuthorCategory = itemView.findViewById(R.id.txt_author_category);
                
                txtLikes = itemView.findViewById(R.id.txt_likes);
                txtRating = itemView.findViewById(R.id.txt_rating);
                txtReadingTime = itemView.findViewById(R.id.txt_reading_time);
                txtPublishedAt = itemView.findViewById(R.id.txt_published_at);
            }
        }
    }
}

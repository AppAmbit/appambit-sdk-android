package com.appambit.javaapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appambit.javaapp.models.CmsExampleModel;
import com.appambit.sdk.Cms;
import com.appambit.sdk.services.interfaces.ICmsQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CmsFragment extends Fragment {
    private static final String CMS_CONTENT_TYPE = "android_blog_posts";

    private CmsAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient imageHttpClient = new OkHttpClient();

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
         loadPosts(Cms.content(CMS_CONTENT_TYPE, CmsExampleModel.class));
    }

    private void setupFilters(View view) {
        Spinner spinner = view.findViewById(R.id.spinner_filters);
        EditText editSearch = view.findViewById(R.id.edit_search);
        Button btnExecute = view.findViewById(R.id.btn_execute);

        String[] options = {
            "All Posts",
            "Title = first",
            "Title ≠ first",
            "Is Published = true",
            "Is Published = false",
            "Title contains 'st'",
            "Title starts with 'f'",
            "Category IN [science]",
            "Category NOT IN [tech, news]",
            "Likes > 500",
            "Rating ≥ 2.1",
            "Reading Time < 15m",
            "Reading Time ≤ 15m",
            "Sort Published At ↑",
            "Sort Published At ↓",
            "Sort Likes ↑",
            "Sort Likes ↓",
            "Page 1 (2 per page)",
            "Page 2 (2 per page)"
        };

        android.widget.ArrayAdapter<String> adapterSpinner = new android.widget.ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, options);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    editSearch.setText("");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && spinner.getSelectedItemPosition() != 0) {
                    spinner.setSelection(0);
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnExecute.setOnClickListener(v -> {
            String text = editSearch.getText().toString().trim();
             ICmsQuery<CmsExampleModel> query = Cms.content(CMS_CONTENT_TYPE, CmsExampleModel.class);

            if (!text.isEmpty()) {
                query = query.search(text);
            }

            switch (spinner.getSelectedItemPosition()) {
                case 0: break;
                case 1: query = query.equals("title", "first"); break;
                case 2: query = query.notEquals("title", "first"); break;
                case 3: query = query.equals("is_published", "true"); break;
                case 4: query = query.equals("is_published", "false"); break;
                case 5: query = query.contains("title", "st"); break;
                case 6: query = query.startsWith("title", "f"); break;
                case 7: query = query.inList("category", List.of("science")); break;
                case 8: query = query.notInList("category", List.of("technology", "news")); break;
                case 9: query = query.greaterThan("likes", 500); break;
                case 10: query = query.greaterThanOrEqual("rating", 2.1); break;
                case 11: query = query.lessThan("reading_time", 15); break;
                case 12: query = query.lessThanOrEqual("reading_time", 15); break;
                case 13: query = query.orderByAscending("published_at"); break;
                case 14: query = query.orderByDescending("published_at"); break;
                case 15: query = query.orderByAscending("likes"); break;
                case 16: query = query.orderByDescending("likes"); break;
                case 17: query = query.getPage(1).getPerPage(2); break;
                case 18: query = query.getPage(2).getPerPage(2); break;
            }
            loadPosts(query);
        });
    }

    private void loadPosts(ICmsQuery<CmsExampleModel> query) {
        query.getList()
             .then(posts -> {
                 if (posts != null && !posts.isEmpty()) {
                     adapter.updatePosts(posts);
                 }
             })
             .onError(error -> {
                 if (getContext() != null) {
                     android.widget.Toast.makeText(getContext(), "Error: " + error.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                 }
             });
    }

    private class CmsAdapter extends RecyclerView.Adapter<CmsAdapter.CmsViewHolder> {
        private final List<CmsExampleModel> posts;

        public CmsAdapter(List<CmsExampleModel> posts) {
            this.posts = posts;
        }

        public void updatePosts(List<CmsExampleModel> newPosts) {
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
            CmsExampleModel post = posts.get(position);
            holder.txtTitle.setText(post.title);
            holder.txtBody.setText(post.body);
            String catStr = post.category != null ? android.text.TextUtils.join(", ", post.category) : "Unknown";
            holder.txtAuthorCategory.setText(post.author + " in " + catStr);

            holder.txtLikes.setText("❤️ " + post.likes);
            holder.txtRating.setText("⭐ " + post.rating + "/5");
            holder.txtReadingTime.setText("📖 " + post.readingTime + " min");
            holder.txtPublishedAt.setText(post.publishedAt != null ? post.publishedAt : "");

            if (post.isPublished != null) {
                holder.txtIsPublished.setVisibility(View.VISIBLE);
                if (post.isPublished) {
                    holder.txtIsPublished.setText("Published");
                    holder.txtIsPublished.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                    holder.txtIsPublished.setBackgroundResource(R.drawable.bg_badge_published);
                } else {
                    holder.txtIsPublished.setText("Draft");
                    holder.txtIsPublished.setTextColor(android.graphics.Color.parseColor("#E65100"));
                    holder.txtIsPublished.setBackgroundResource(R.drawable.bg_badge_draft);
                }
            } else {
                holder.txtIsPublished.setVisibility(View.GONE);
            }

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
                    Request request = new Request.Builder().url(url).get().build();
                    Bitmap bmp;
                    try (Response response = imageHttpClient.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            handler.post(() -> imageView.setVisibility(View.GONE));
                            return;
                        }
                        bmp = BitmapFactory.decodeStream(response.body().byteStream());
                    }
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
            TextView txtLikes, txtRating, txtReadingTime, txtPublishedAt, txtIsPublished;

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
                txtIsPublished = itemView.findViewById(R.id.txt_is_published);
            }
        }
    }
}

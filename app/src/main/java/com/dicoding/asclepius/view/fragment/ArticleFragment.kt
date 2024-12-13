package com.dicoding.asclepius.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.ArticleAdapter
import com.dicoding.asclepius.data.response.ArticlesItem
import com.dicoding.asclepius.data.retrofit.ApiConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleFragment : Fragment() {

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var recyclerView: RecyclerView
    private val articles = mutableListOf<ArticlesItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_article, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        articleAdapter = ArticleAdapter(articles)
        recyclerView.adapter = articleAdapter

        fetchArticles()
    }

    private fun fetchArticles() {
        val client = ApiConfig.getApiService().getTopHeadlines(
            country = "us",
            category = "health",
            apiKey = "ecb0ef4b7c6849a5add1c5dd348cc178"
        )
        client.enqueue(object : Callback<com.dicoding.asclepius.data.response.Response> {
            override fun onResponse(
                call: Call<com.dicoding.asclepius.data.response.Response>,
                response: Response<com.dicoding.asclepius.data.response.Response>
            ) {
                if (response.isSuccessful) {
                    response.body()?.articles?.let {
                        articles.clear()

                        val filteredArticles = it.filter { article ->
                            article?.title != "[Removed]"
                        }

                        articles.addAll(filteredArticles.filterNotNull())
                        articleAdapter.notifyDataSetChanged()
                    }
                }
            }
            override fun onFailure(call: Call<com.dicoding.asclepius.data.response.Response>, t: Throwable) {}
        })
    }


}

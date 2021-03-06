package com.suihan74.hatena.api

import com.suihan74.hatena.entry.*
import com.suihan74.hatena.exception.HttpException
import com.suihan74.hatena.exception.InvalidResponseException
import com.suihan74.hatena.extension.queryParameters
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URI
import java.net.URLEncoder

/**
 * エントリ関連のAPI
 */
interface EntryService {
    /**
     * カテゴリを指定して人気/新着エントリを取得する
     *
     * @param entriesType 人気or新着
     * @param category カテゴリ
     * @param limit 最大件数
     * @param offset 取得開始位置
     * @param includeAMPUrls AMP用のURLを含める
     * @param includeBookmarkedData 自分のブクマ情報を含める(サインイン済みの場合)
     * @param includeBookmarksOfFollowings フォローしているユーザーのブクマ情報を含める(サインイン済みの場合)
     * @param includeAds はてなから提供される広告を含める
     */
    @GET("api/ipad.{type}.json")
    suspend fun getEntries(
        @Path("type") @EntriesTypeQuery(EntriesTypeUsage.ENTRIES) entriesType: EntriesType,
        @Query("category_id") category: Category,
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null,
        @Query("include_amp_urls") includeAMPUrls: Boolean = true,
        @Query("include_bookmarked_data") includeBookmarkedData: Boolean = true,
        @Query("include_bookmarks_of_followings") includeBookmarksOfFollowings: Boolean = true,
        @Query("ad") includeAds: Boolean = false
    ) : List<EntryItem>

    // ------ //

    /**
     * Issueを指定して人気/新着エントリを取得する
     *
     * @param entriesType 人気or新着
     * @param issue 特集
     * @param limit 最大件数
     * @param offset 取得開始位置
     * @param includeAMPUrls AMP用のURLを含める
     * @param includeBookmarkedData 自分のブクマ情報を含める(サインイン済みの場合)
     * @param includeBookmarksByVisitor
     * @param includeBookmarksOfFollowings フォローしているユーザーのブクマ情報を含める(サインイン済みの場合)
     * @param includeAds はてなから提供される広告を含める
     */
    @GET("api/internal/cambridge/issue/{issue_id}/{type}")
    suspend fun getIssueEntries(
        @Path("type") @EntriesTypeQuery(EntriesTypeUsage.ISSUE_ENTRIES) entriesType: EntriesType,
        @Path("issue_id") issue: Issue,
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null,
        @Query("include_amp_urls") includeAMPUrls: Boolean = true,
        @Query("include_bookmarked_data") includeBookmarkedData: Boolean = true,
        @Query("include_bookmarks_by_visitor") includeBookmarksByVisitor: Boolean = true,
        @Query("include_bookmarks_of_followings") includeBookmarksOfFollowings: Boolean = true,
        @Query("ad") includeAds: Boolean = false
    ) : IssueEntriesResponse

    /**
     * 指定カテゴリの特集一覧を取得する
     */
    @GET("api/internal/cambridge/category/{category_id}/issues")
    suspend fun getIssues(@Path("category_id") category: Category) : IssuesResponse

    // ------ //

    /**
     * エントリをクエリ検索する
     */
    @GET("api/ipad.search/{searchType}")
    suspend fun searchEntries(
        @Path("searchType") searchType: SearchType,
        @Query("q") query: String,
        @Query("sort") @EntriesTypeQuery(EntriesTypeUsage.SEARCH_SORT) sortType: EntriesType = EntriesType.RECENT,
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null,
        @Query("include_bookmarked_data") includeBookmarkedData: Boolean = true
    ) : List<EntryItem>

    // ------ //

    /**
     * 指定ユーザーがブクマしたエントリを取得する
     *
     * @see EntryService.getBookmarkedEntries
     */
    @GET("api/internal/user/{user}/bookmarks")
    suspend fun __getBookmarkedEntries(
        @Path("user") user: String,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null
    ) : UserEntryResponse

    // ------ //

    /**
     * 指定URLのブクマ数を取得する（同時最大50件）
     *
     * ユーザーが利用するために同一のAPIを`BookmarkService`にも用意しているが、
     * 当クラスのものは`EntryService#getHistoricalEntries`で使用するためのもの
     */
    @GET("https://bookmark.hatenaapis.com/count/entries")
    suspend fun __getBookmarksCount(
        @Query("url") urls: List<String>
    ) : Map<String, Int>

    /**
     * 15周年ページのはてな全体の過去人気エントリリストを取得する
     *
     * ブクマ数は別途取得する必要がある
     */
    @GET("15th/entries/{year}.json")
    suspend fun __getHistoricalEntries(
        @Path("year") year: Int
    ) : HatenaHistoricalEntry
}

suspend fun EntryService.getHistoricalEntries(year: Int) : List<Entry> {
    val entries = __getHistoricalEntries(year).entries
    val bookmarksCounts = HatenaClient.bookmark.getBookmarksCount(entries.map { it.canonicalUrl })
    return entries.map { entry ->
        entry.toEntry(count = bookmarksCounts.getOrDefault(entry.canonicalUrl, 0))
    }
}

/**
 * 指定ユーザーがブクマしたエントリを取得する
 */
suspend fun EntryService.getBookmarkedEntries(
    user: String,
    tag: String? = null,
    limit: Int? = null,
    offset: Int? = null
) : List<UserEntry> = __getBookmarkedEntries(user, tag, limit, offset).bookmarks

/**
 * 与えられたページのfaviconのURLを取得する
 *
 * @return faviconのURL(実際にはてながキャッシュしていて画像が取得できるかは考慮しない)
 */
fun EntryService.getFaviconUrl(url: String) : String =
    "https://cdn-ak2.favicon.st-hatena.com/?url=${URLEncoder.encode(url, "UTF-8")}"


/**
 * 指定ページのエントリIDを取得する
 *
 * @throws HttpException 通信失敗
 * @throws InvalidResponseException レスポンスの処理に失敗
 */
suspend fun EntryService.getEntryId(url: String) : Long {
    val entryUrl = HatenaClient.getEntryUrl(url)
    return HatenaClient.generalService.getHtml(entryUrl) { html ->
        html.getElementsByTag("html")!!
            .first()
            .attr("data-entry-eid")
            .toLong()
    }
}

/**
 * エントリIDから対象のページのURLを取得する
 *
 * @param eid エントリID
 * @return 対象ページのURL
 * @throws HttpException 通信失敗
 */
suspend fun EntryService.getUrl(eid: Long) : String {
    val baseUrl = HatenaClient.baseUrlB
    val eidEntryUrl = buildString { append(baseUrl, "entry/", eid) }
    return HatenaClient.generalService.get(eidEntryUrl).let { response ->
        if (!response.isSuccessful) throw HttpException(response)
        val entryUrl = response.raw().request.url.toString()
        val headHttps = "${baseUrl}entry/s/"
        val isHttps = entryUrl.startsWith(headHttps)
        val scheme =
            if (isHttps) "https://"
            else "http://"
        val tail = entryUrl.substring(
            if (isHttps) headHttps.length
            else headHttps.length - 2
        )

        "$scheme$tail"
    }
}

/**
 * エントリURLから対象ページのURLを取得する
 *
 * @param entryUrl エントリページのURL
 * @return エントリの対象となっている元ページのURL
 * @throws IllegalArgumentException 渡されたurlがエントリURLとして判別不可能
 *
 * cases
 * 1) https://b.hatena.ne.jp/entry/s/www.hoge.com/ ==> https://www.hoge.com/
 * 2) https://b.hatena.ne.jp/entry/https://www.hoge.com/ ==> https://www.hoge.com/
 * 3) https://b.hatena.ne.jp/entry/{eid}/comment/{username} ==> https://b.hatena.ne.jp/entry/{eid}  (modifySpecificUrls()を参照)
 * 4) https://b.hatena.ne.jp/entry?url=https~~~
 * 5) https://b.hatena.ne.jp/entry?eid=1234
 * 6) https://b.hatena.ne.jp/entry/{eid}
 * 7) https://b.hatena.ne.jp/entry.touch/s/~~~
 * 8) https://b.hatena.ne.jp/entry/panel/?url=~~~
 */
fun EntryService.getUrl(entryUrl: String) : String {
    val baseUrl = HatenaClientBase.baseUrlB
    if (entryUrl.startsWith("${baseUrl}entry?url=") || entryUrl.startsWith("${baseUrl}entry/panel/?url=")) {
        // 4, 8)
        return URI.create(entryUrl).queryParameters["url"] ?: throw IllegalArgumentException("invalid comment page url: $entryUrl")
    }
    else if (entryUrl.startsWith("${baseUrl}entry?eid=")) {
        // 5)
        val eid = URI.create(entryUrl).queryParameters["eid"] ?: throw IllegalArgumentException("invalid comment page url: $entryUrl")
        return "${baseUrl}entry/$eid"
    }

    val commentUrlRegex = Regex("""https?://b\.hatena\.ne\.jp/entry/(\d+)(/comment/\w+)?""")
    val commentUrlMatch = commentUrlRegex.matchEntire(entryUrl)
    if (commentUrlMatch != null) {
        // 3, 6)
        return "${baseUrl}entry/${commentUrlMatch.groups[1]!!.value}"
    }

    val regex = Regex("""https?://b\.hatena\.ne\.jp/entry(\.touch)?/(https://|s/)?(.+)""")
    val matches = regex.matchEntire(entryUrl) ?: throw IllegalArgumentException("invalid comment page url: $entryUrl")
    val path = matches.groups[3]?.value ?: throw IllegalArgumentException("invalid comment page url: $entryUrl")

    // 1,2)
    return if (matches.groups[2]?.value.isNullOrEmpty()) {
        if (path.startsWith("http://")) path // 2)
        else "http://$path" // 1)
    }
    else "https://$path"
}

/**
 * 対象ページのURLからエントリページのURLを取得する
 *
 * @param url エントリの対象となっている元ページのURL
 * @return エントリページのURL
 * @throws IllegalArgumentException "http"or"https"スキーム以外の文字列が渡された場合
 */
fun EntryService.getEntryUrl(url: String) : String = buildString {
    append("${HatenaClientBase.baseUrlB}entry/")
    append(
        when {
            url.startsWith("https://") -> "s/${url.substring("https://".length)}"
            url.startsWith("http://") -> url.substring("http://".length)
            else -> throw IllegalArgumentException("invalid url: $url")
        }
    )
}

/**
 * 対象ページのタイトルを取得する
 */
suspend fun EntryService.getPageTitle(url: String) : String =
    HatenaClient.generalService.getHtml(url) { doc ->
        val titleTag = doc.getElementsByTag("title").firstOrNull()
        titleTag?.wholeText().orEmpty()
    }

// ------ //

/**
 * 認証が必要なエントリ関係API
 */
interface CertifiedEntryService : EntryService {
    val accountName : String

    /**
     * サインインユーザーがブクマしたエントリ一覧を取得する
     *
     * @param limit 最大件数
     * @param offset 取得開始位置
     * @throws HttpException 通信失敗
     */
    @GET("api/ipad.mybookmarks")
    suspend fun getBookmarkedEntries(
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null
    ) : List<EntryItem>

    /**
     * サインインユーザーがブクマしたエントリを検索する
     */
    @GET("api/ipad.mysearch/{search_type}")
    suspend fun searchBookmarkedEntries(
        @Path("search_type") searchType: SearchType,
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("of") offset: Int? = null
    ) : List<EntryItem>

    /**
     * マイホットエントリを取得する
     */
    @GET("api/entries/myhotentry.json")
    suspend fun getMyHotEntries(
        @Query("date") date: String? = null,
        @Query("include_amp_urls") includeAMPUrls: Boolean = true
    ) : List<MyHotEntry>

    @GET("api/my/15th/yearly_random_bookmarks")
    suspend fun __getUserHistoricalEntries(
        @Query("year") year: Int,
        @Query("limit") limit: Int = 10
    ) : List<UserHistoricalEntry>
}

class CertifiedEntryServiceImpl(delegate : CertifiedEntryService) : CertifiedEntryService by delegate {
    override lateinit var accountName: String
}

/**
 * ユーザーの15周年タイムカプセルエントリを取得する
 *
 * @param year 2011 ~ 2020
 */
suspend fun CertifiedEntryService.getUserHistoricalEntries(year: Int, limit: Int = 10) : List<Entry> {
    return __getUserHistoricalEntries(year, limit).map {
        it.toEntry(accountName)
    }
}

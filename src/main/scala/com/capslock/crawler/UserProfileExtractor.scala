package com.capslock.crawler

import com.capslock.crawler.UserProfile.{FEMALE, MALE}
import org.jsoup.Jsoup

import scala.io.Source

case class UserProfileExtractor(username: String, url: String, body: String) {
    def extract(): Option[UserProfile] = {
        val doc = Jsoup.parse(body)
        val titleSectionDiv = doc.getElementsByClass("title-section").first()
        val nickname = titleSectionDiv.getElementsByClass("name").first().text()
        val biology = doc.getElementsByClass("bio").first().text()
        val avatars = doc.select(s"img[alt=$nickname]")
        var userAvatar = ""
        for (i <- 0 until avatars.size()) {
            val avatar = avatars.get(i)
            if (avatar.className().equals("Avatar Avatar--l")) {
                userAvatar = avatar.attr("src")
            }
        }
        val locationItemGroup = doc.select("div.item.editable-group").select("[data-name=location]")
        val locationItem = locationItemGroup.select("span.location.item").first()
        val businessItem = locationItemGroup.select("span.business.item").first()
        val genderItem = locationItemGroup.select("span.item.gender").first()
        val location = if (locationItem.hasAttr("title")) locationItem.attr("title") else ""
        val business = if (businessItem.hasAttr("title")) businessItem.attr("title") else ""
        val gender = if (genderItem.child(0).className().contains("female")) FEMALE else MALE

        val employmentItemGroup = doc.select("div.item.editable-group").select("[data-name=employment]")
        val companyItem = employmentItemGroup.select("span.employment.item").first()
        val positionItem = employmentItemGroup.select("span.position.item").first()
        val company = if (companyItem.hasAttr("title")) companyItem.attr("title") else ""
        val position = if (positionItem.hasAttr("title")) positionItem.attr("title") else ""

        val eduItemGroup = doc.select("div.item.editable-group").select("[data-name=education]")
        val schoolItem = eduItemGroup.select("span.education.item").first()
        val majorItem = eduItemGroup.select("span.education-extra.item").first()
        val school = if (schoolItem != null && schoolItem.hasAttr("title")) schoolItem.attr("title") else ""
        val major = if (majorItem != null && majorItem.hasAttr("title")) majorItem.attr("title") else ""

        val descriptionGroup = doc.select("div.zm-profile-header-description.editable-group")
        val descriptionItem = descriptionGroup.select("span.content")
        val description = if (descriptionItem != null) descriptionItem.text() else ""

        val hash = doc.select("button[data-follow=m:button]").attr("data-id")
        val infoList = doc.select("div.zm-profile-header-info-list").first()
        val agrees = infoList.select("span.zm-profile-header-user-agree").text().replace("赞同", "").toLong
        val thanks = infoList.select("span.zm-profile-header-user-thanks").text().replace("感谢", "").toLong

        val activeGroup = doc.select("div.profile-navbar.clearfix")
        val asks = activeGroup.select("a.item").select("[href$=asks]").select("span.num").text().toLong
        val answers = activeGroup.select("a.item").select("[href$=answers]").select("span.num").text().toLong
        val posts = activeGroup.select("a.item").select("[href$=posts]").select("span.num").text().toLong
        val collections = activeGroup.select("a.item").select("[href$=collections]").select("span.num").text().toLong
        val logs = activeGroup.select("a.item").select("[href$=logs]").select("span.num").text().toLong

        val followingGroup = doc.select("div.zm-profile-side-following.zg-clear")
        val followees = followingGroup.select("[href$=followees]").select("strong").text().toLong
        val followers = followingGroup.select("[href$=followers]").select("strong").text().toLong

        val userProfile = UserProfile(username, nickname, url, biology, userAvatar, location, business, company, position,
            school, major, description, hash, agrees, thanks, asks, answers, posts, collections, logs, followees, followers)
        Some(userProfile)
    }
}

object UserProfileExtractor {

    def main(args: Array[String]): Unit = {
        val data = Source.fromFile("G:\\zhihu.html", "UTF-8").getLines().mkString("\n")
        val doc = Jsoup.parse(data)
        val titleSectionDiv = doc.getElementsByClass("title-section").first()
        val nickname = titleSectionDiv.getElementsByClass("name").first().text()
        val biology = doc.getElementsByClass("bio").first().text()
        val avatars = doc.select(s"img[alt=$nickname]")
        var userAvatar = ""
        for (i <- 0 until avatars.size()) {
            val avatar = avatars.get(i)
            if (avatar.className().equals("Avatar Avatar--l")) {
                userAvatar = avatar.attr("src")
            }
        }
        val locationItemGroup = doc.select("div.item.editable-group").select("[data-name=location]")
        val locationItem = locationItemGroup.select("span.location.item").first()
        val businessItem = locationItemGroup.select("span.business.item").first()
        val genderItem = locationItemGroup.select("span.item.gender").first()
        val location = if (locationItem.hasAttr("title")) locationItem.attr("title") else ""
        val business = if (businessItem.hasAttr("title")) businessItem.attr("title") else ""
        val gender = if (genderItem.child(0).className().contains("female")) FEMALE else MALE

        val employmentItemGroup = doc.select("div.item.editable-group").select("[data-name=employment]")
        val companyItem = employmentItemGroup.select("span.employment.item").first()
        val positionItem = employmentItemGroup.select("span.position.item").first()
        val company = if (companyItem.hasAttr("title")) companyItem.attr("title") else ""
        val position = if (positionItem.hasAttr("title")) positionItem.attr("title") else ""

        val eduItemGroup = doc.select("div.item.editable-group").select("[data-name=education]")
        val schoolItem = eduItemGroup.select("span.education.item").first()
        val majorItem = eduItemGroup.select("span.education-extra.item").first()
        val school = if (schoolItem != null && schoolItem.hasAttr("title")) schoolItem.attr("title") else ""
        val major = if (majorItem != null && majorItem.hasAttr("title")) majorItem.attr("title") else ""

        val descriptionGroup = doc.select("div.zm-profile-header-description.editable-group")
        val descriptionItem = descriptionGroup.select("span.content")
        val description = if (descriptionItem != null) descriptionItem.text() else ""

        val hash = doc.select("button[data-follow=m:button]").attr("data-id")
        val infoList = doc.select("div.zm-profile-header-info-list").first()
        val agrees = infoList.select("span.zm-profile-header-user-agree").text().replace("赞同", "")
        val thanks = infoList.select("span.zm-profile-header-user-thanks").text().replace("感谢", "")

        val activeGroup = doc.select("div.profile-navbar.clearfix")
        val asks = activeGroup.select("a.item").select("[href$=asks]").select("span.num").text()
        val answers = activeGroup.select("a.item").select("[href$=answers]").select("span.num").text()
        val posts = activeGroup.select("a.item").select("[href$=posts]").select("span.num").text()
        val collections = activeGroup.select("a.item").select("[href$=collections]").select("span.num").text()
        val logs = activeGroup.select("a.item").select("[href$=logs]").select("span.num").text()

        val followingGroup = doc.select("div.zm-profile-side-following.zg-clear")
        val followees = followingGroup.select("[href$=followees]").select("strong").text()
        val followers = followingGroup.select("[href$=followers]").select("strong").text()
        println(nickname)
        println(biology)
        println(userAvatar)
        println(location)
        println(business)
        println(gender)
        println(company)
        println(position)
        println(school)
        println(major)
        println(description)
        println(hash)
        println(agrees)
        println(thanks)
        println(asks)
        println(answers)
        println(posts)
        println(collections)
        println(logs)
        println(followees)
        println(followers)
    }
}

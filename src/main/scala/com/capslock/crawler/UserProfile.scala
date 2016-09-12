package com.capslock.crawler

/**
 * Created by capslock1874.
 */
case class UserProfile(username: String, nickname: String, link: String, biology: String, avatar: String,
    location: String, business: String, company: String, position: String, school: String, major: String,
    description: String, hash: String, agrees: Long, thanks: Long, asks: Long, answers: Long, posts: Long,
    collections: Long, logs: Long, followees: Long, followers: Long)

object UserProfile {

    sealed trait GENDER {
        def value: Int
    }

    case object FEMALE extends GENDER {
        def value = 0
    }

    case object MALE extends GENDER {
        def value = 1
    }

}
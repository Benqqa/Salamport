package com.newpage.salamport.requests

import com.vk.api.sdk.requests.VKRequest
import org.json.JSONObject

data class VKUser(
    val name: String,
    val surname: String,
    val town: String,
    val bdate: String,
    val country: String,
    val patronymic: String = ""
)
class UserRequest(uids: IntArray = intArrayOf()) : VKRequest<VKUser>("account.getProfileInfo") {

    override fun parse(r: JSONObject): VKUser {
        val user = r.getJSONObject("response")
        val res = VKUser(user.getString("first_name"),
            user.getString("last_name"),
            user.getJSONObject("city").getString("title"),
            user.getString("bdate"),
            user.getJSONObject("country").getString("title")
        )
        return res
    }
}

class AvatarRequest: VKRequest<String> {
    constructor(uids: IntArray = intArrayOf()):super("users.get") {
        if (uids.isNotEmpty()) {
            addParam("user_ids", uids.joinToString(","))
        }
        addParam("fields", "photo_200")
    }

    override fun parse(r: JSONObject): String {
        val users = r.getJSONArray("response")
        val result = users.getJSONObject(0).getString("photo_200")
        return result
    }
}



package com.github.kr328.clash.design.adapter

import com.github.kr328.clash.service.model.Profile

/**
 * Inline action handlers for profile card chips (更新/编辑/复制/删除).
 * Replaces the old BottomSheetDialog + ⋮ menu pattern.
 */
interface ProfileActions {
    fun onUpdate(profile: Profile)
    fun onEdit(profile: Profile)
    fun onDuplicate(profile: Profile)
    fun onDelete(profile: Profile)
}

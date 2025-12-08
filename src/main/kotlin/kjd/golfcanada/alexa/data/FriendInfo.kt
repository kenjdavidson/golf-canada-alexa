package kjd.golfcanada.alexa.data

import kjd.golfcanada.alexa.util.HasFriendName

/**
 * Lightweight representation of a friend containing only essential information.
 * 
 * This class is used for session storage to minimize data size, containing only
 * the friend's member ID, full name, and handicap rather than the complete Friend DTO.
 * 
 * @property memberId The friend's unique member identifier
 * @property name The friend's full name
 * @property handicap The friend's handicap index (optional)
 */
data class FriendInfo(
    val memberId: Long?,
    override val name: String?,
    val handicap: String? = null
) : HasFriendName

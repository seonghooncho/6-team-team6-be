package ktb.billage.fixture;

import ktb.billage.contract.token.TokenGenerator;
import ktb.billage.domain.chat.ChatMessage;
import ktb.billage.domain.chat.ChatMessageRepository;
import ktb.billage.domain.chat.Chatroom;
import ktb.billage.domain.chat.ChatroomRepository;
import ktb.billage.domain.group.Group;
import ktb.billage.domain.group.GroupRepository;
import ktb.billage.domain.membership.Membership;
import ktb.billage.domain.membership.MembershipRepository;
import ktb.billage.domain.post.Post;
import ktb.billage.domain.post.PostImage;
import ktb.billage.domain.post.PostImageRepository;
import ktb.billage.domain.post.PostRepository;
import ktb.billage.domain.user.User;
import ktb.billage.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

@Component
public class Fixtures {
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private ChatroomRepository chatroomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public String 토큰_생성(User user) {
        return tokenGenerator.generateAccessToken(user.getId());
    }

    public User 유저_생성() {
        return userRepository.save(UserFixture.one(null, null));
    }

    public User 유저_생성(String loginId) {
        return userRepository.save(UserFixture.one(loginId, null));
    }

    public User 유저_생성(String loginId, String password) {
        return userRepository.save(UserFixture.one(loginId, password));
    }

    public User 또_다른_유저_생성() {
        return userRepository.save(UserFixture.one("user-" + System.nanoTime(), null));
    }

    public Group 그룹_생성(String groupName) {
        return groupRepository.save(GroupFixture.one(groupName));
    }

    public Group 그룹_기본_커버_생성(String groupCoverImage) {
        return groupRepository.save(GroupFixture.defaultCover(groupCoverImage));
    }

    public Membership 그룹_가입(Group group, User user) {
        return membershipRepository.save(MembershipFixture.one(group, user));
    }

    public void 그룹_탈퇴(Group group, User user) {
        Membership membership = membershipRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(group.getId(), user.getId()).get();

        membership.delete(Instant.now());
        membershipRepository.save(membership);
    }

    public List<Long> 소속_그룹_벌크_생성(User user, int count) {
        List<Long> membershipIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO billage_group (group_name, group_cover_image_url, created_at, updated_at, deleted_at) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
                    "group-" + i,
                    "dummy.png"
            );
            Long groupId = jdbcTemplate.queryForObject(
                    "SELECT id FROM billage_group WHERE group_name = ?",
                    Long.class,
                    "group-" + i
            );

            jdbcTemplate.update(
                    "INSERT INTO membership (group_id, user_id, created_at, updated_at, deleted_at) " +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
                    groupId,
                    user.getId()
            );
            Long membershipId = jdbcTemplate.queryForObject(
                    "SELECT id FROM membership WHERE group_id = ? AND user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    groupId,
                    user.getId()
            );
            membershipIds.add(membershipId);
        }
        return membershipIds;
    }

    public void 그룹원_벌크_생성(Group group, int count) {
        for (int i = 1; i <= count; i++) {
            String loginId = "bulk_user_" + group.getId() + "_" + i;
            jdbcTemplate.update(
                    "INSERT INTO users (login_id, password, avatar_url, created_at, updated_at, deleted_at) " +
                            "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
                    loginId,
                    "test-password",
                    "images/default-avatar.png"
            );
            Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login_id = ?",
                    Long.class,
                    loginId
            );
            jdbcTemplate.update(
                    "INSERT INTO membership (group_id, user_id, nickname, created_at, updated_at, deleted_at) " +
                            "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
                    group.getId(),
                    userId,
                    "nick-" + i
            );
        }
    }

    public Post 게시글_생성(Membership membership) {
        return 게시글_생성(membership, 1);
    }

    public Post 게시글_생성(Membership membership, int value) {
        Post post = postRepository.save(PostFixture.one(membership));
        postImageRepository.save(new PostImage(post, "img-" + value, 1));
        게시글_수정시간_설정(post, value);
        return post;
    }

    public List<Post> 여러_게시글_생성(Membership membership, int count) {
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            posts.add(게시글_생성(membership, i));
        }
        return posts;
    }

    public void 여러_게시글_제목_수정(List<Post> posts, String newTitle) {
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", newTitle)
                .addValue("ids", postIds);

        namedParameterJdbcTemplate.update(
                "UPDATE post SET title = :title WHERE id IN (:ids)",
                params
        );
    }

    public void 게시글_일부_삭제(List<Post> posts, IntPredicate shouldDelete) {
        for (int i = 0; i < posts.size(); i++) {
            if (shouldDelete.test(i)) {
                게시글_삭제(posts.get(i));
            }
        }
    }

    public void 게시글_삭제(Post post) {
        post.delete(Instant.now());
        postRepository.save(post);
    }

    private void 게시글_수정시간_설정(Post post, int value) {
        Instant adjusted = BASE_TIME.plusSeconds(value * 1000L);

        jdbcTemplate.update("UPDATE post SET updated_at = ? WHERE id = ?", adjusted, post.getId());
    }

    public Chatroom 채팅방_생성(Post post, Membership buyerMembership) {
        return chatroomRepository.save(new Chatroom(post.getId(), buyerMembership.getId()));
    }

    public ChatMessage 채팅_전송(Chatroom chatroom, Membership senderMembership, int sendAtPlusOffset) {
        Instant sendAt = BASE_TIME.plusSeconds(sendAtPlusOffset * 1000L);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(senderMembership.getId(), chatroom, "message", BASE_TIME.plusSeconds(sendAtPlusOffset * 1000L)));

        chatroom.sendMessage(message.getId(), senderMembership.getId(), sendAt);
        chatroomRepository.save(chatroom);

        return message;
    }
}

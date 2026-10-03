# Nexus (Phase 5)

Kotlin + Jetpack Compose + Firebase Auth/Firestore + Cloudinary (images).

## Debug SHA-1 (add this in Firebase)
CF:C4:A6:11:4D:22:06:74:39:36:0F:1A:79:4A:C9:9E:FB:56:20:CB

## Setup
1. Firebase: new project, add Android app with package `com.nexus.app`, add the SHA-1 above.
2. Authentication: enable Email/Password and Google.
3. Firestore: create database, then paste `firestore.rules` in the Rules tab and Publish.
4. Download `google-services.json` and put it at `app/google-services.json`.
5. Cloudinary: copy your cloud name, create an UNSIGNED upload preset, and put both
   in `app/src/main/java/com/nexus/app/Config.kt`.
6. Push to GitHub. Actions builds the APK: Actions tab > latest run > Artifacts.

## Firestore schema (relationships)
users/{uid}           username, name, bio, avatarUrl, coverUrl, createdAt     [Phase 1]
usernames/{username}  uid                                                      [Phase 1]
posts/{postId}        authorId -> users, text, imageUrl, createdAt, likeCount, commentCount  [Phase 2 done]
  comments/{id}       authorId -> users, text, createdAt                       [Phase 2 done]
  likes/{uid}         createdAt  (doc id = liker's uid, so no double likes)    [Phase 2 done]
friendships/{a_b}     requesterId, addresseeId -> users, members[a,b], status, createdAt  [Phase 3 done]
                      (id = the two user ids joined with _; status pending/accepted;
                       decline / cancel / unfriend = delete the document)
chats/{a_b}           members[a,b], lastMessage, lastMessageAt, lastSenderId,
                      unread{uid:count}, lastRead{uid:time}                    [Phase 4 done]
  messages/{id}       senderId -> users, text, createdAt                       [Phase 4 done]
users/{uid}/notifications/{id}  actorId -> users, type, postId, read, createdAt  [Phase 4 done]

## One-time Firestore index (needed for the friends-only feed, Phase 3)
Firestore > Indexes > Composite > Add index
  Collection ID: posts | Fields: authorId (Ascending), createdAt (Descending) | Query scope: Collection
Wait until its status says Enabled.

## Making yourself an admin (Phase 5)
1. Firebase > Authentication > Users: copy your "User UID".
2. Firestore > Start collection > Collection ID: admins > Document ID: paste your UID
   > add any field (for example role = admin) > Save.
3. Reopen the app: your Profile tab now shows an "Admin panel" button.
To lift a ban: Firestore > banned > delete that person's document.

## More collections (Phase 5)
users/{uid}/blocked/{id}   createdAt                                           [Phase 5 done]
reports/{uid_type_target}  reporterId, targetType, targetId, targetUserId, reason, details, status, createdAt  [Phase 5 done]
admins/{uid}               (you create by hand)                               [Phase 5 done]
banned/{uid}               by, createdAt                                      [Phase 5 done]

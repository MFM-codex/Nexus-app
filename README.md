# Nexus (Phase 9)

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

## Releasing (Phase 6)
Every push builds two files on GitHub (Actions > latest run > Artifacts):
  nexus-debug-apk    for testing
  nexus-release-apk  the one to share with friends (faster, not a debug build)
Both are signed with app/nexus-debug.keystore, so one installs over the other and
Google sign-in keeps working. For the Play Store you would create a private release key
(never commit it) and store it in GitHub Secrets.

## Security checklist
- Every write is checked by firestore.rules (you can only change your own data).
- Text, field names and sizes are validated in the rules, not just in the app.
- Posts, comments, chats, likes and notifications are limited to friends.
- Admin and ban powers are controlled by documents only you can create (admins/banned).
- Posting is rate-limited to once per 10 seconds (rateLimits/{uid}).
- Not covered (needs the paid plan or Play Store): App Check, push notifications, auto-deleting
  old comments/likes when a post is deleted.

## Reels (Phase 8)
reels/{id}            authorId, videoUrl (Cloudinary), caption, createdAt, likeCount, commentCount
  likes/{uid}, comments/{id}   same idea as posts
reelLimits/{uid}      lastReelAt  (one reel every 30 seconds)
Reels are public to every signed-in user. Videos play exactly as uploaded (max 60 s, 100 MB).

## Menu (Phase 9)
The 4th tab is now Menu (hamburger): profile, shortcuts, Saved, Settings & privacy, Help & support, Log out.
users/{uid}/saved/{postId}   savedAt   (your private saved posts)
Settings (theme, data saver) are stored on the phone, not in Firebase.
Groups, Pages, Marketplace and Events are shown as "coming soon".

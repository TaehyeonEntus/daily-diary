코드리뷰 해야함.

+ 인기글 캐시 어떻게 할지
+ comment service에 있는 스펙 분석
+ Cascade 어떻게 할건지, User 사라지면 Post랑Comment 둘 다 지우기. Post 사라지면 PostLike,Comment 지우기. Comment 사라지면 CommentLike 지우기. - DB단 FK 옵션
+ 댓글 리스트는 직접 쿼리 작성하기
+ N+1 검사하기
+ Cascade로 삭제 시 likeCount를 어떻게 처리할 것 인지? -> 
+ - JPA에서 모든 엔티티 조회 후 삭제 -> 접근 횟수가 NxN으로 늘어남
- 스프링 배치로 chunk 처리 -> 그 정도로 필요하지는 않을 것 같음
- groupBy후 update쿼리 -> DB내부에서 처리 되는거라 빠르다. 이거로 스케쥴링 하면 될 것 같음.
+ 인기글 like count로 정렬 이후 createdAt 1일 걸고 검색.
+ Post에 조회수 추가 및 원자적 연산으로 업데이트

T22까지 완료

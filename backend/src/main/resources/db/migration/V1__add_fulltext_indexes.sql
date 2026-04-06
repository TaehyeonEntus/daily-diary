ALTER TABLE posts ADD FULLTEXT INDEX ft_idx_posts_title (title) WITH PARSER ngram;
ALTER TABLE posts ADD FULLTEXT INDEX ft_idx_posts_content (content) WITH PARSER ngram;
ALTER TABLE posts ADD FULLTEXT INDEX ft_idx_posts_author (author) WITH PARSER ngram;
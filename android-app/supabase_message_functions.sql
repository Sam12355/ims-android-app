-- Create RPC functions to fetch messages and threads
-- Run this in Supabase SQL Editor

-- Function to get all messages for a user
CREATE OR REPLACE FUNCTION get_user_messages(user_id_param uuid)
RETURNS TABLE (
  id uuid,
  sender_id uuid,
  receiver_id uuid,
  content text,
  created_at timestamptz,
  read_at timestamptz,
  thread_id uuid,
  fcm_message_id text
)
LANGUAGE plpgsql
SECURITY DEFINER -- Run with elevated privileges to bypass RLS
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    m.id,
    m.sender_id,
    m.receiver_id,
    m.content,
    m.created_at,
    m.read_at,
    m.thread_id,
    m.fcm_message_id
  FROM messages m
  WHERE m.sender_id = user_id_param 
     OR m.receiver_id = user_id_param
  ORDER BY m.created_at DESC;
END;
$$;

-- Function to get all threads for a user
CREATE OR REPLACE FUNCTION get_user_threads(user_id_param uuid)
RETURNS TABLE (
  id uuid,
  user1_id uuid,
  user2_id uuid,
  last_message_id uuid,
  updated_at timestamptz,
  created_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER -- Run with elevated privileges to bypass RLS
AS $$
BEGIN
  RETURN QUERY
  SELECT 
    t.id,
    t.user1_id,
    t.user2_id,
    t.last_message_id,
    t.updated_at,
    t.created_at
  FROM threads t
  WHERE t.user1_id = user_id_param 
     OR t.user2_id = user_id_param
  ORDER BY t.updated_at DESC;
END;
$$;

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION get_user_messages(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION get_user_messages(uuid) TO anon;
GRANT EXECUTE ON FUNCTION get_user_threads(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION get_user_threads(uuid) TO anon;

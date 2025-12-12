-- Function to mark messages as read
-- This function bypasses RLS to allow marking messages as read
-- Run this in Supabase SQL Editor

CREATE OR REPLACE FUNCTION mark_messages_as_read(
  p_reader_id uuid,
  p_sender_id uuid
)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER -- Run with elevated privileges to bypass RLS
AS $$
DECLARE
  updated_count integer;
BEGIN
  -- Update all unread messages from sender to reader
  UPDATE messages
  SET read_at = NOW()
  WHERE receiver_id = p_reader_id
    AND sender_id = p_sender_id
    AND read_at IS NULL;
  
  GET DIAGNOSTICS updated_count = ROW_COUNT;
  
  RETURN updated_count;
END;
$$;

-- Grant execute permissions to authenticated and anon users
GRANT EXECUTE ON FUNCTION mark_messages_as_read(uuid, uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION mark_messages_as_read(uuid, uuid) TO anon;

-- Test the function (optional):
-- SELECT mark_messages_as_read('reader-uuid-here', 'sender-uuid-here');

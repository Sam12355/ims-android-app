-- Create RPC function to update user photo_url
-- Run this in Supabase SQL Editor

CREATE OR REPLACE FUNCTION update_user_photo(
  user_id_param uuid,
  new_photo_url text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER -- Run with elevated privileges
AS $$
BEGIN
  -- Update the users table
  UPDATE users
  SET photo_url = new_photo_url
  WHERE id = user_id_param;
  
  -- Check if the update was successful
  IF NOT FOUND THEN
    RETURN json_build_object(
      'success', false,
      'error', 'User not found'
    );
  END IF;
  
  -- Return success
  RETURN json_build_object(
    'success', true,
    'photo_url', new_photo_url
  );
END;
$$;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION update_user_photo(uuid, text) TO authenticated;
GRANT EXECUTE ON FUNCTION update_user_photo(uuid, text) TO anon;

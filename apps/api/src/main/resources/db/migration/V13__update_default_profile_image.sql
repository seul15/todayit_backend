ALTER TABLE member
    ALTER COLUMN profile_image
        SET DEFAULT 'https://res.cloudinary.com/xmb52jrp/image/upload/v1793191339/default_profile.png';

UPDATE member
SET profile_image = 'https://res.cloudinary.com/xmb52jrp/image/upload/v1793191339/default_profile.png'
WHERE profile_image = '/images/default-profile.png';
